import { Participant } from "@Models";
import axios from "axios";
import config from "../config";

async function getParticipants(): Promise<null | Participant[]> {
  try {
    const response = await axios.get(`${config.apiHost}/network/participants`);
    return response.data;
  } catch (error) {
    console.error(error);
    return null;
  }
}

export default getParticipants;
