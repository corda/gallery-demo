import { GalleryLot, Participant } from "../models";

export function getParticipantPath(participant: Participant): string {
  const paths: { [string: string]: string } = {
    "ParticipantType.GalleryOwner": "gallery",
    "ParticipantType.Bidder": "bidder",
  };
  return `/${paths[participant.type]}/${convertToKebabCase(participant.displayName)}`;
}

export function timeout(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export function convertToKebabCase(string: string) {
  return string.replace(/\s+/g, "-").toLowerCase();
}

export function lotSold(lot: GalleryLot): boolean {
  return lot.bids
    .map((bid) => bid.accepted)
    .reduce((accumulatedStatus, currentStatus) => accumulatedStatus || currentStatus, false);
}
