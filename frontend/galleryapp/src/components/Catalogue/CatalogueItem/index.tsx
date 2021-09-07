import styles from "./styles.module.scss";
import { GalleryLot } from "../../../models";
import { useState } from "react";
import CatalogueItemBids from "@Components/Catalogue/CatalogueItemBids";
import { ReactComponent as Chevron } from "@Assets/chevronIcon.svg";

interface Props {
  lot: GalleryLot;
}

function CatalogueItem({ lot }: Props) {
  const [isToggled, setToggled] = useState(false);

  return (
    <>
      <tr className={styles.main} onClick={() => setToggled(!isToggled)}>
        <td className={`${styles.chevron} ${isToggled ? styles.open : ""}`}>
          <Chevron />
        </td>
        <td>{lot.displayName}</td>
        <td>{lot.id}</td>
        <td>{lot.reservePrice}</td>
        <td>{lot.bids.length}</td>
        <td>TBC</td>
      </tr>

      {isToggled && <CatalogueItemBids bids={lot.bids} />}
    </>
  );
}

export default CatalogueItem;
