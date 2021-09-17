import styles from "./styles.module.scss";
import { GalleryLot } from "@Models";
import { useState } from "react";
import CatalogueItemBids from "@Components/Catalogue/CatalogueItemBids";
import { ReactComponent as Chevron } from "@Assets/chevronIcon.svg";
import { Badge } from "@r3/r3-tooling-design-system";
import { lotSold } from "@Utils";

interface Props {
  lot: GalleryLot;
}

function CatalogueItem({ lot }: Props) {
  const [isToggled, setToggled] = useState(false);
  const sold = lotSold(lot);

  return (
    <>
      <tr className={styles.main} onClick={() => setToggled(!isToggled)}>
        <td className={`${styles.chevron} ${isToggled ? styles.open : ""}`}>
          <Chevron />
        </td>
        <td>
          <div className={styles.lotDetail}>
            <img className={styles.thumbnail} src={lot.url} alt={lot.description} />
            {lot.description}
          </div>
        </td>
        <td>{lot.artworkId}</td>
        <td>{lot.expiryDate}</td>
        <td>{lot.bids.length}</td>
        <td>{sold ? <Badge variant="green">Sold</Badge> : <Badge variant="gray">Open</Badge>}</td>
      </tr>

      {isToggled && <CatalogueItemBids bids={lot.bids} lotId={lot.artworkId} open={!sold} />}
    </>
  );
}

export default CatalogueItem;
