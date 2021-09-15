import React, { createContext, FC, useState } from "react";
import { GalleryLot } from "@Models";
import useInterval from "@Hooks/useInterval";
import {getGallery} from "@Api";
import { isEqual } from "lodash";

interface ArtworksContextInterface {
  artworks: GalleryLot[];
}

const contextDefaultValues: ArtworksContextInterface = {
  artworks: [],
};

export const ArtworksContext = createContext<ArtworksContextInterface>(contextDefaultValues);

export const ArtworksProvider: FC = ({ children }) => {
  const [artworks, setArtwork] = useState<GalleryLot[]>(contextDefaultValues.artworks);

  useInterval(async () => {
    const lots = await getGallery();
    if (lots && !isEqual(lots, artworks)) setArtwork(lots);
  }, 2000);

  return (
    <ArtworksContext.Provider
      value={{
        artworks,
      }}
    >
      {children}
    </ArtworksContext.Provider>
  );
};
