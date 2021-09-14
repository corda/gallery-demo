import axios from "axios";
import config from "../config";

async function postBid(): Promise<string | null> {
  try {
    const response = await axios.get(`${config.apiHost}/network/log`);
    return response.data;
  } catch (error) {
    console.error(error);
    return null;
  }
}

export default postBid;
