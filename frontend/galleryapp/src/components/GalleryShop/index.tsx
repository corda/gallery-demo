import styles from "./styles.module.scss";
import ActivityLog from "@Components/ActivityLog";
import { Badge } from "@r3/r3-tooling-design-system";
import GalleryBidModal from "@Components/GalleryShop/GalleryBidModal";
import { useContext, useState } from "react";
import { Bid, GalleryLot, RouterParams } from "@Models";
import { LogsContext } from "@Context/logs";
import { getWinningBid, usersBid } from "@Utils";
import { useParams } from "react-router-dom";
import { UsersContext } from "@Context/users";
import { isEqual } from "lodash";

interface Props {
  lots: GalleryLot[];
  x500: string;
}

function GalleryShop({ lots, x500 }: Props) {
  const { getUser } = useContext(UsersContext);
  const { getFilteredLogs } = useContext(LogsContext);
  const [bidModalToggle, setBidModalToggle] = useState(false);
  const [selectedItem, setSelectedItem] = useState<GalleryLot | null>(null);
  const logs = getFilteredLogs(x500, "auction");
  const { id } = useParams<RouterParams>();
  const user = getUser(id);

  if (!user) return null;

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
        {lots
            .sort((a, b) => {
                return a.description < b.description ? -1 : 1;
            })
            .map((lot) => (
          <ShopItem
            key={lot.artworkId}
            lot={lot}
            onClick={() => handleItemClick(lot)}
            usersBid={usersBid(lot, user)}
          />
        ))}
      </ul>
      <ActivityLog title="Gallery activity log" inline={true} logs={logs} />
      <GalleryBidModal
        open={bidModalToggle}
        onClose={() => handleClose()}
        selectedArtwork={selectedItem}
        user={user}
      />
    </section>
  );
}

interface ShopItemProps {
  lot: GalleryLot;
  onClick: () => void;
  usersBid: Bid | undefined;
}

function ShopItem({ lot, onClick, usersBid }: ShopItemProps) {
  const winningBid = getWinningBid(lot);

  return (
    <li className={`${styles.lotItem} ${!!winningBid ? styles.lotItemSold : ""}`} onClick={onClick}>
      <img className={styles.itemImagePlaceholder} src={lot.url} alt={lot.description} />
      <h6>{lot.description}</h6>
      {winningBid && isEqual(winningBid, usersBid) && (
        <Badge variant="green" className={styles.lotItemBadge}>
          BOUGHT{" "}
          <span>
            ({winningBid.amount} {winningBid.currencyCode})
          </span>
        </Badge>
      )}
      {winningBid && !isEqual(winningBid, usersBid) && (
        <Badge variant="green" className={styles.lotItemBadge}>
          SOLD{" "}
          <span>
            ({winningBid.amount} {winningBid.currencyCode})
          </span>
        </Badge>
      )}
      {!winningBid && usersBid && (
        <Badge variant="gray" className={styles.lotItemBadge}>
          BID PLACED{" "}
          <span>
            ({usersBid.amount} {usersBid.currencyCode})
          </span>
        </Badge>
      )}
      {!winningBid && !usersBid && (
        <Badge variant="gray" className={styles.lotItemBadge}>
          FOR SALE
        </Badge>
      )}
    </li>
  );
}

export default GalleryShop;
