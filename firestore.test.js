const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

test("Unauthenticated users cannot read or write any Nova collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("commands").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("history").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("commands").doc("cmd_1").set({
      userId: ALICE_UID,
      phrase: "Morning brief",
      category: "REMINDERS",
      actionsSummary: "Read news and weather",
      actionSteps: ["Read weather", "Read calendar"],
      permissionPolicy: "ALWAYS_ALLOW",
      usageCount: 0,
      enabled: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("UserSettings: owner create, partial update, ghost field & cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      wakeWord: "Hey Nova",
      voiceGender: "FEMALE",
      voiceSpeed: 1.0,
      language: "EN",
      personality: "FRIENDLY",
      themeMode: "DARK",
      listeningMode: "ALWAYS_ON",
      alwaysListening: true,
      runInBackground: true,
      startOnBoot: true,
      keepAlive: true,
      onDeviceProcessing: false,
      speakerRecognition: true,
      autoDeleteDays: 30,
      pinConfirmationEnabled: true,
      appLockEnabled: false,
      emergencyContact: "+1 555-0199",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Partial update of mutable fields succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      wakeWord: "Nova Prime",
      language: "UR",
      updatedAt: pastDate(1000),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(500),
    })
  );

  // Cross-user read & write fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      wakeWord: "Hijack",
      updatedAt: pastDate(500),
    })
  );
});

test("CustomCommands: owner create/list/update/delete, @DocumentId id rejection, cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("commands").doc("cmd_1").set({
      userId: ALICE_UID,
      phrase: "Movie night",
      category: "CONNECTIVITY",
      actionsSummary: "Enable DND, dim brightness, open YouTube",
      actionSteps: ["Enable DND", "Set brightness 20%", "Open YouTube"],
      permissionPolicy: "ALWAYS_ALLOW",
      usageCount: 3,
      enabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("commands").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("commands").doc("cmd_1").update({
      usageCount: 4,
      enabled: false,
      updatedAt: pastDate(1000),
    })
  );

  // Reject extra @DocumentId 'id' field on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("commands").doc("cmd_2").set({
      id: "cmd_2",
      userId: ALICE_UID,
      phrase: "Invalid ID",
      category: "APPS",
      actionsSummary: "Open Spotify",
      actionSteps: ["Open Spotify"],
      permissionPolicy: "ALWAYS_ALLOW",
      usageCount: 0,
      enabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("commands").doc("cmd_3").set({
      userId: ALICE_UID,
      phrase: "Future",
      category: "APPS",
      actionsSummary: "Open Spotify",
      actionSteps: ["Open Spotify"],
      permissionPolicy: "ALWAYS_ALLOW",
      usageCount: 0,
      enabled: true,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("commands").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("commands").doc("cmd_1").delete());
});

test("Automations, History, and SmartDevices: owner operations succeed & cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("automations").doc("auto_1").set({
      userId: ALICE_UID,
      title: "Good Morning",
      triggerType: "TIME",
      triggerDetail: "07:00 AM Daily",
      actionSteps: ["Disable DND", "Read daily briefing", "Set volume 65%"],
      enabled: true,
      runCount: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("history").doc("hist_1").set({
      userId: ALICE_UID,
      userQuery: "Turn on WiFi and open Spotify",
      novaReply: "WiFi enabled and launching Spotify.",
      intentTag: "MULTI_STEP",
      executedActions: ["WiFi ON", "Open Spotify"],
      modeUsed: "NORMAL",
      bookmarked: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("history").doc("hist_1").update({
      bookmarked: true,
      updatedAt: pastDate(1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("smartDevices").doc("dev_1").set({
      userId: ALICE_UID,
      name: "Studio Lights",
      room: "Living Room",
      deviceType: "LIGHT",
      poweredOn: true,
      levelValue: 80,
      voiceEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("smartDevices").doc("dev_1").update({
      poweredOn: false,
      levelValue: 0,
      updatedAt: pastDate(1000),
    })
  );

  // Bob cannot list Alice's subcollections
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("automations").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("history").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("smartDevices").get());
});
