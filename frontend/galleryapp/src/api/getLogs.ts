import { Log } from "@Models";
import axios from "axios";
import config from "../config";

async function getLogs(): Promise<Log[] | null> {
  try {
    const response = await axios.get(`${config.apiHost}/network/log`);
    return response.data;
  } catch (error) {
    console.error(error);
    return null;
  }
}

export default getLogs;
