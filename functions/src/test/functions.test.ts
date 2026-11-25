import assert from "assert";
import {
  validateEmergencyRequestInput,
  EmergencyProcessRequest
} from "../emergencyProcessor";

console.log("Running Cloud Functions Unit Tests...");

// Test 1: Valid input passes validation
{
  const validReq: EmergencyProcessRequest = {
    emergencyId: "em_123",
    journeyId: "j_456",
    userId: "u_789"
  };
  const res = validateEmergencyRequestInput(validReq);
  assert.strictEqual(res.valid, true, "Valid request should pass");
  assert.strictEqual(res.errors.length, 0, "Errors should be empty");
  console.log("✓ Test 1: Valid emergency input passes validation");
}

// Test 2: Missing emergencyId rejected
{
  const invalidReq = {
    emergencyId: "",
    journeyId: "j_456",
    userId: "u_789"
  };
  const res = validateEmergencyRequestInput(invalidReq);
  assert.strictEqual(res.valid, false, "Missing emergencyId should fail");
  assert.ok(res.errors.some((e: string) => e.includes("emergencyId")), "Error message mentions emergencyId");
  console.log("✓ Test 2: Missing emergencyId rejected");
}

// Test 3: Missing journeyId rejected
{
  const invalidReq = {
    emergencyId: "em_123",
    journeyId: "  ",
    userId: "u_789"
  };
  const res = validateEmergencyRequestInput(invalidReq);
  assert.strictEqual(res.valid, false, "Blank journeyId should fail");
  assert.ok(res.errors.some((e: string) => e.includes("journeyId")), "Error message mentions journeyId");
  console.log("✓ Test 3: Blank journeyId rejected");
}

// Test 4: Missing userId rejected
{
  const invalidReq = {
    emergencyId: "em_123",
    journeyId: "j_456",
    userId: undefined
  };
  const res = validateEmergencyRequestInput(invalidReq as any);
  assert.strictEqual(res.valid, false, "Undefined userId should fail");
  assert.ok(res.errors.some((e: string) => e.includes("userId")), "Error message mentions userId");
  console.log("✓ Test 4: Missing userId rejected");
}

console.log("All Cloud Functions tests passed successfully!");
