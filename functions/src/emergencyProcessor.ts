/**
 * Input request schema for processing an emergency event on the server.
 */
export interface EmergencyProcessRequest {
  emergencyId: string;
  journeyId: string;
  userId: string;
}

/**
 * Result of emergency event validation and processing.
 */
export interface EmergencyProcessResult {
  valid: boolean;
  errors: string[];
  emergencyId: string;
  journeyId: string;
  userId: string;
  contactCount?: number;
}

/**
 * Validates the core structure of an EmergencyProcessRequest before querying Firestore.
 */
export function validateEmergencyRequestInput(input: Partial<EmergencyProcessRequest>): { valid: boolean; errors: string[] } {
  const errors: string[] = [];

  if (!input.emergencyId || typeof input.emergencyId !== "string" || input.emergencyId.trim() === "") {
    errors.push("Invalid or missing emergencyId");
  }

  if (!input.journeyId || typeof input.journeyId !== "string" || input.journeyId.trim() === "") {
    errors.push("Invalid or missing journeyId");
  }

  if (!input.userId || typeof input.userId !== "string" || input.userId.trim() === "") {
    errors.push("Invalid or missing userId");
  }

  return {
    valid: errors.length === 0,
    errors,
  };
}

/**
 * Core validation logic for emergency event processing against Firestore records.
 * Ensures:
 * 1. The emergency exists.
 * 2. The journey belongs to the user.
 * 3. The emergency belongs to the journey.
 * 4. Selected contacts belong to the user's trusted contacts.
 */
export async function validateEmergencyRecords(
  db: FirebaseFirestore.Firestore,
  request: EmergencyProcessRequest
): Promise<EmergencyProcessResult> {
  const errors: string[] = [];
  const { emergencyId, journeyId, userId } = request;

  // 1. Verify Emergency exists
  const emergencyDoc = await db.collection("emergencies").doc(emergencyId).get();
  if (!emergencyDoc.exists) {
    errors.push(`Emergency document '${emergencyId}' does not exist`);
    return { valid: false, errors, emergencyId, journeyId, userId };
  }

  const emergencyData = emergencyDoc.data();
  if (!emergencyData) {
    errors.push("Emergency document data is empty");
    return { valid: false, errors, emergencyId, journeyId, userId };
  }

  // Verify emergency belongs to user
  if (emergencyData.userId !== userId) {
    errors.push(`Emergency does not belong to user '${userId}'`);
  }

  // 2. Verify Journey belongs to the user
  const journeyDoc = await db.collection("journeys").doc(journeyId).get();
  if (!journeyDoc.exists) {
    errors.push(`Journey document '${journeyId}' does not exist`);
    return { valid: false, errors, emergencyId, journeyId, userId };
  }

  const journeyData = journeyDoc.data();
  if (!journeyData || journeyData.userId !== userId) {
    errors.push(`Journey '${journeyId}' does not belong to user '${userId}'`);
  }

  // 3. Verify Emergency belongs to the journey
  if (emergencyData.journeyId !== journeyId) {
    errors.push(`Emergency '${emergencyId}' does not reference journey '${journeyId}'`);
  }

  // 4. Verify selected contacts belong to user's trusted contacts
  const selectedContactIds: string[] = Array.isArray(emergencyData.selectedContactIds)
    ? emergencyData.selectedContactIds
    : [];

  let validContactCount = 0;
  if (selectedContactIds.length > 0) {
    const trustedContactsSnapshot = await db
      .collection("users")
      .doc(userId)
      .collection("trustedContacts")
      .get();

    const userTrustedContactIds = new Set(trustedContactsSnapshot.docs.map((d) => d.id));

    for (const contactId of selectedContactIds) {
      if (!userTrustedContactIds.has(contactId)) {
        errors.push(`Selected contact '${contactId}' is not among user's trusted contacts`);
      } else {
        validContactCount++;
      }
    }
  }

  return {
    valid: errors.length === 0,
    errors,
    emergencyId,
    journeyId,
    userId,
    contactCount: validContactCount,
  };
}
