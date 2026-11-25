"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.processEmergencyEvent = void 0;
const admin = __importStar(require("firebase-admin"));
const functions = __importStar(require("firebase-functions"));
const emergencyProcessor_1 = require("./emergencyProcessor");
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
exports.processEmergencyEvent = functions.https.onCall(async (data, context) => {
    // Require authentication
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Authentication is required to process emergency events");
    }
    // Input validation
    const inputValidation = (0, emergencyProcessor_1.validateEmergencyRequestInput)(data);
    if (!inputValidation.valid) {
        throw new functions.https.HttpsError("invalid-argument", `Validation failed: ${inputValidation.errors.join("; ")}`);
    }
    const request = {
        emergencyId: data.emergencyId,
        journeyId: data.journeyId,
        userId: data.userId,
    };
    // Caller must match userId
    if (context.auth.uid !== request.userId) {
        throw new functions.https.HttpsError("permission-denied", "You can only process emergencies for your own account");
    }
    try {
        const result = await (0, emergencyProcessor_1.validateEmergencyRecords)(db, request);
        if (!result.valid) {
            throw new functions.https.HttpsError("failed-precondition", `Emergency validation failed: ${result.errors.join("; ")}`);
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
    }
    catch (err) {
        if (err instanceof functions.https.HttpsError)
            throw err;
        throw new functions.https.HttpsError("internal", err?.message || "Server error");
    }
});
//# sourceMappingURL=index.js.map