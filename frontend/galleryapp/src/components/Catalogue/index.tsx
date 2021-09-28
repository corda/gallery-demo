import styles from "./styles.module.scss";
import { GalleryLot } from "@Models";
import CatalogueItem from "@Components/Catalogue/CatalogueItem";
import ActivityLog from "@Components/ActivityLog";
import { useContext } from "react";
import { LogsContext } from "@Context/logs";

interface Props {
  lots: GalleryLot[];
}

function Catalogue({ lots }: Props) {
  const { getFilteredLogs } = useContext(LogsContext);
  const logs = getFilteredLogs(null, "AUCTION");

  return (
    <section className={styles.main}>
      <h3>Catalogue</h3>
      <table>
        <thead>
          <tr>
            <th />
            <th>Lot</th>
            <th>Lot Id</th>
            <th>Expiry Date</th>
            <th># Bids</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {[...lots]
            .sort((a, b) => {
              return a.description < b.description ? -1 : 1;
            })
            .map((lot) => (
              <CatalogueItem key={lot.artworkId} lot={lot} />
            ))}
        </tbody>
      </table>
      <div className={styles.log}>
        <ActivityLog title="Auction Audit Log" inline={true} logs={logs} />
      </div>
    </section>
  );
}

export default Catalogue;
