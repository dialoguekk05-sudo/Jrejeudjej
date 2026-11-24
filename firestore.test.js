const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

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

test("Unauthenticated users cannot read or write profiles, messages, or reminders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      preferredLanguage: "en",
      mood: "Focused",
      voiceEnabled: true,
      ttsSpeechRate: 1.0,
      backgroundAssistantEnabled: false,
      cityName: "New Delhi",
      memorySummary: "",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("messages").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("reminders").get());
});

test("Owner can create and partially update UserProfile while rejecting ghost fields and immutable mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Aarav",
      preferredLanguage: "hinglish",
      mood: "Focused",
      voiceEnabled: true,
      ttsSpeechRate: 1.0,
      backgroundAssistantEnabled: false,
      cityName: "Mumbai",
      memorySummary: "Prefers concise Hindi-English answers.",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update on mood, cityName, and updatedAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      mood: "Study",
      cityName: "Bengaluru",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt tamper
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject invalid speech rate (> 2.0)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      ttsSpeechRate: 5.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field on create
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      preferredLanguage: "en",
      mood: "Chill",
      voiceEnabled: true,
      ttsSpeechRate: 1.0,
      backgroundAssistantEnabled: false,
      cityName: "Delhi",
      memorySummary: "",
      isAdmin: true,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Cross-user isolation prevents Bob from reading or modifying Alice's profile, messages, or reminders", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      preferredLanguage: "en",
      mood: "Focused",
      voiceEnabled: true,
      ttsSpeechRate: 1.0,
      backgroundAssistantEnabled: false,
      cityName: "Delhi",
      memorySummary: "Private memory",
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("users").doc(ALICE_UID).collection("messages").doc("msg_1").set({
      userId: ALICE_UID,
      role: "user",
      text: "Hello Hayat",
      language: "en",
      hasImage: false,
      createdAt: created,
    });
    await db.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_1").set({
      userId: ALICE_UID,
      title: "Study Physics",
      timeLabel: "7:00 PM",
      category: "Study",
      completed: false,
      createdAt: created,
      updatedAt: created,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("messages").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("reminders").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_1").update({
      completed: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Messages support Kotlin create payloads (with and without actionTag), ordered list query, and delete; reject @DocumentId 'id' field", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Without optional actionTag (null filtered out in Kotlin)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("m1").set({
      userId: ALICE_UID,
      role: "user",
      text: "Subah 7 baje ka alarm laga do",
      language: "hi",
      hasImage: false,
      createdAt: created,
    })
  );

  // With actionTag
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("m2").set({
      userId: ALICE_UID,
      role: "model",
      text: "Bilkul! Maine subah 7:00 AM ka alarm set kar diya hai.",
      language: "hi",
      actionTag: "SET_ALARM",
      hasImage: false,
      createdAt: new Date(Date.now() - 2000),
    })
  );

  // Exact Kotlin query: orderBy("createdAt").limitToLast(40)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").orderBy("createdAt", "asc").limitToLast(40).get()
  );

  // Reject extra @DocumentId `id` field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("m3").set({
      id: "m3",
      userId: ALICE_UID,
      role: "user",
      text: "Invalid id field",
      language: "en",
      hasImage: false,
      createdAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("m4").set({
      userId: ALICE_UID,
      role: "user",
      text: "Future timestamp",
      language: "en",
      hasImage: false,
      createdAt: new Date(Date.now() + 60000),
    })
  );

  // Delete allowed by owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("m1").delete()
  );
});

test("Reminders support create, partial toggle update, list query, and delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 4000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reminders").doc("r1").set({
      userId: ALICE_UID,
      title: "Revise Thermodynamics",
      timeLabel: "08:30 PM",
      category: "Study",
      completed: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reminders").doc("r1").update({
      completed: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reminders").orderBy("createdAt", "desc").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reminders").doc("r1").delete()
  );
});
