import { Participant, ParticipantType } from "../models";

async function getUsers(): Promise<Participant[]> {
  await timeout(2000);
  return [
    {
      displayName: "Alice GalleryShop",
      id: "0",
      wallets: [],
      type: ParticipantType.GalleryOwner,
    },
    {
      displayName: "Bob XDC",
      id: "1",
      wallets: [],
      type: ParticipantType.Patron,
    },
    {
      displayName: "Charlie GBP",
      id: "2",
      wallets: [],
      type: ParticipantType.Patron,
    },
  ];
}

function timeout(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export default getUsers;
