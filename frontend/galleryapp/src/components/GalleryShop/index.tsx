import styles from "./styles.module.scss";
import ActivityLog from "@Components/ActivityLog";
import { ReactComponent as Image } from "@Assets/imageIcon.svg";
import { Badge } from "@r3/r3-tooling-design-system";
import GalleryBidModal from "@Components/GalleryShop/GalleryBidModal";
import { useState } from "react";
import { GalleryLot } from "../../models";
import { mockGalleryLots } from "../../mockData";

function GalleryShop() {
  const [bidModalToggle, setBidModalToggle] = useState(false);
  const [selectedItem, setSelectedItem] = useState<GalleryLot | null>(null);

  const handleItemClick = (lot: GalleryLot) => {
    setBidModalToggle(true);
    setSelectedItem(lot);
  };

  const handleClose = () => {
    console.log("testsaedfasda");
    setBidModalToggle(false);
    setSelectedItem(null);
  };

  return (
    <section className={styles.main}>
      <h2>Gallery</h2>
      <ul className={styles.lotList}>
        {mockGalleryLots.map((lot) => (
          <ShopItem key={lot.id} lot={lot} onClick={() => handleItemClick(lot)} />
        ))}
      </ul>
      <ActivityLog title="Gallery activity log" small={true} />
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
  return (
    <li className={styles.lotItem} onClick={onClick}>
      <Image className={styles.itemImagePlaceholder} />
      <h6>{lot.displayName}</h6>
      <Badge variant="gray">
        {lot.reservePrice} {lot.currencySymbol}
      </Badge>
    </li>
  );
}

export default GalleryShop;
