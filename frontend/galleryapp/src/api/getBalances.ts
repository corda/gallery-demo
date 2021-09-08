import { Balance } from "@Models";
import axios from "axios";
import config from "../config";

async function getBalances(): Promise<Balance[] | null> {
  try {
    const response = await axios.get(`${config.apiHost}/network/balance`);
    return response.data;
  } catch (error) {
    console.error(error);
    return null;
  }
}

export default getBalances;
