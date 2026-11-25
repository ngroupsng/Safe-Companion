import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
import {
  EmergencyProcessRequest,
  validateEmergencyRequestInput,
  validateEmergencyRecords,
} from "./emergencyProcessor";

// Initialize Firebase Admin SDK
if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

/**
 * Cloud Function: processEmergencyEvent
 *
 * Validates:
 * 1. Emergency exists
 * 2. Journey belongs to user
 * 3. Emergency belongs to journey
 * 4. Selected contacts belong to user's trusted contacts
 *
 * Prepares server-side pipeline for trusted contact notifications via FCM.
 */
export const processEmergencyEvent = functions.https.onCall(
  async (data: Partial<EmergencyProcessRequest>, context) => {
    // Require authentication
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Authentication is required to process emergency events"
      );
    }

    // Input validation
    const inputValidation = validateEmergencyRequestInput(data);
    if (!inputValidation.valid) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        `Validation failed: ${inputValidation.errors.join("; ")}`
      );
    }

    const request: EmergencyProcessRequest = {
      emergencyId: data.emergencyId!,
      journeyId: data.journeyId!,
      userId: data.userId!,
    };

    // Caller must match userId
    if (context.auth.uid !== request.userId) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "You can only process emergencies for your own account"
      );
    }

    try {
      const result = await validateEmergencyRecords(db, request);
      if (!result.valid) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          `Emergency validation failed: ${result.errors.join("; ")}`
        );
      }

      // Mark emergency as verified by server
      await db.collection("emergencies").doc(request.emergencyId).update({
        serverProcessedAt: admin.firestore.FieldValue.serverTimestamp(),
        serverVerificationStatus: "VERIFIED",
      });

      return {
        success: true,
        emergencyId: request.emergencyId,
        journeyId: request.journeyId,
        contactCount: result.contactCount,
        status: "VERIFIED",
      };
    } catch (err: any) {
      if (err instanceof functions.https.HttpsError) throw err;
      throw new functions.https.HttpsError("internal", err?.message || "Server error");
    }
  }
);
