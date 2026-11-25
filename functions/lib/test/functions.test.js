"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const assert_1 = __importDefault(require("assert"));
const emergencyProcessor_1 = require("../emergencyProcessor");
console.log("Running Cloud Functions Unit Tests...");
// Test 1: Valid input passes validation
{
    const validReq = {
        emergencyId: "em_123",
        journeyId: "j_456",
        userId: "u_789"
    };
    const res = (0, emergencyProcessor_1.validateEmergencyRequestInput)(validReq);
    assert_1.default.strictEqual(res.valid, true, "Valid request should pass");
    assert_1.default.strictEqual(res.errors.length, 0, "Errors should be empty");
    console.log("✓ Test 1: Valid emergency input passes validation");
}
// Test 2: Missing emergencyId rejected
{
    const invalidReq = {
        emergencyId: "",
        journeyId: "j_456",
        userId: "u_789"
    };
    const res = (0, emergencyProcessor_1.validateEmergencyRequestInput)(invalidReq);
    assert_1.default.strictEqual(res.valid, false, "Missing emergencyId should fail");
    assert_1.default.ok(res.errors.some((e) => e.includes("emergencyId")), "Error message mentions emergencyId");
    console.log("✓ Test 2: Missing emergencyId rejected");
}
// Test 3: Missing journeyId rejected
{
    const invalidReq = {
        emergencyId: "em_123",
        journeyId: "  ",
        userId: "u_789"
    };
    const res = (0, emergencyProcessor_1.validateEmergencyRequestInput)(invalidReq);
    assert_1.default.strictEqual(res.valid, false, "Blank journeyId should fail");
    assert_1.default.ok(res.errors.some((e) => e.includes("journeyId")), "Error message mentions journeyId");
    console.log("✓ Test 3: Blank journeyId rejected");
}
// Test 4: Missing userId rejected
{
    const invalidReq = {
        emergencyId: "em_123",
        journeyId: "j_456",
        userId: undefined
    };
    const res = (0, emergencyProcessor_1.validateEmergencyRequestInput)(invalidReq);
    assert_1.default.strictEqual(res.valid, false, "Undefined userId should fail");
    assert_1.default.ok(res.errors.some((e) => e.includes("userId")), "Error message mentions userId");
    console.log("✓ Test 4: Missing userId rejected");
}
console.log("All Cloud Functions tests passed successfully!");
//# sourceMappingURL=functions.test.js.map