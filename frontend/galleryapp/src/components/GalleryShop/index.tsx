import styles from "./styles.module.scss";
import ActivityLog from "@Components/ActivityLog";
import { Badge } from "@r3/r3-tooling-design-system";
import GalleryBidModal from "@Components/GalleryShop/GalleryBidModal";
import { useContext, useState } from "react";
import { GalleryLot } from "@Models";
import { LogsContext } from "@Context/logs";
import { lotSold } from "@Helpers";

interface Props {
  lots: GalleryLot[];
  x500: string;
}

function GalleryShop({ lots, x500 }: Props) {
  const { getFilteredLogs } = useContext(LogsContext);
  const [bidModalToggle, setBidModalToggle] = useState(false);
  const [selectedItem, setSelectedItem] = useState<GalleryLot | null>(null);
  const logs = getFilteredLogs(x500, "auction");

  const handleItemClick = (lot: GalleryLot) => {
    setBidModalToggle(true);
    setSelectedItem(lot);
  };

  const handleClose = () => {
    setBidModalToggle(false);
    setSelectedItem(null);
  };

  return (
    <section className={styles.main}>
      <h3>Gallery</h3>
      <ul className={styles.lotList}>
        {lots.map((lot) => (
          <ShopItem key={lot.artworkId} lot={lot} onClick={() => handleItemClick(lot)} />
        ))}
      </ul>
      <ActivityLog title="Gallery activity log" inline={true} logs={logs} />
      <GalleryBidModal
        open={bidModalToggle}
        onClose={() => handleClose()}
        selectedBid={selectedItem}
      />
    </section>
  );
}

interface ShopItemProps {
  lot: GalleryLot;
  onClick: () => void;
}

function ShopItem({ lot, onClick }: ShopItemProps) {
  const sold = lotSold(lot);

  function getSoldForPrice(lot: GalleryLot) {
    const winningBid = lot.bids.find((bid) => bid.accepted);
    if (!winningBid) return "";

    return `${winningBid.amount} ${winningBid.currencyCode}`;
  }

  return (
    <li className={`${styles.lotItem} ${sold ? styles.lotItemSold : ""}`} onClick={onClick}>
      <img className={styles.itemImagePlaceholder} src={lot.url} alt={lot.description} />
      <h6>{lot.description}</h6>
      {sold ? (
        <Badge variant="green">SOLD ({getSoldForPrice(lot)})</Badge>
      ) : (
        <Badge variant="gray">FOR SALE</Badge>
      )}
    </li>
  );
}

export default GalleryShop;
