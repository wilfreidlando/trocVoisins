const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_cameroon";
const BOB_UID = "bob_cameroon";
const MEDIATOR_UID = "mediator_admin";

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

test("Unauthenticated user: cannot read items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("items").get());
});

test("Unauthenticated user: cannot create user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc("test_user").set({
      userId: "test_user",
      name: "Jean",
      email: "jean@example.cm",
      role: "CITOYEN",
      city: "Douala",
      neighborhood: "Akwa",
      grainesBalance: 5,
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: can create own profile with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice Douala",
      email: "alice@example.cm",
      role: "CITOYEN",
      city: "Douala",
      neighborhood: "Akwa",
      landmark: "Carrefour Salle des Fêtes",
      grainesBalance: 5,
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot impersonate another user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      name: "Bob Impersonated",
      email: "bob@example.cm",
      role: "CITOYEN",
      city: "Yaoundé",
      neighborhood: "Bastos",
      grainesBalance: 5,
      createdAt: new Date(),
    })
  );
});

test("Item lifecycle: Alice creates an item, Bob reads it and reserves it", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const itemId = "item_sac_scolaire_01";

  // Alice publishes an item
  await assertSucceeds(
    aliceDb.collection("items").doc(itemId).set({
      id: itemId,
      ownerId: ALICE_UID,
      ownerName: "Alice Douala",
      title: "Sac à dos d'écolier",
      description: "Sac solide pour écolier du primaire, bon état",
      category: "Scolaire",
      condition: "BON_ETAT",
      exchangeType: "DON",
      city: "Douala",
      neighborhood: "Akwa",
      status: "DISPONIBLE",
      createdAt: new Date(),
    })
  );

  // Bob reads the catalog
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("items").doc(itemId).get());

  // Bob reserves the item
  await assertSucceeds(
    bobDb.collection("items").doc(itemId).update({
      status: "RESERVE",
      reservedByUserId: BOB_UID,
    })
  );
});

test("Dispute lifecycle: Alice reports an issue; only Alice or Mediator can view it", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const disputeId = "disp_001";

  await assertSucceeds(
    aliceDb.collection("disputes").doc(disputeId).set({
      id: disputeId,
      itemId: "item_sac_01",
      itemTitle: "Sac à dos",
      reportedByUserId: ALICE_UID,
      reporterName: "Alice Douala",
      targetUserId: BOB_UID,
      reason: "Objet non conforme à la description",
      status: "OUVERT",
      createdAt: new Date(),
    })
  );

  // Alice can read her dispute
  await assertSucceeds(aliceDb.collection("disputes").doc(disputeId).get());

  // Charlie (stranger) cannot read Alice's dispute
  const charlieDb = testEnv.authenticatedContext("charlie_stranger").firestore();
  await assertFails(charlieDb.collection("disputes").doc(disputeId).get());
});
