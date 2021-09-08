import { GalleryLot } from "@Models";
import axios from "axios";
import config from "../config";

async function getGallery(userDisplayName?: string): Promise<GalleryLot[] | null> {
  const queryString = userDisplayName ? `?user=${userDisplayName}` : "";

  try {
    const response = await axios.get(
      `${config.apiHost}/gallery/list-available-artworks${queryString}`
    );
    return response.data;
  } catch (error) {
    console.error(error);
    return null;
  }
}

export default getGallery;
