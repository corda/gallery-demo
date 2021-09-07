import { Participant } from "../models";

export function getParticipantPath(participant: Participant): string {
  const path = ["gallery", "patron"];

  return `/${path[participant.type]}/${participant.id}`;
}
