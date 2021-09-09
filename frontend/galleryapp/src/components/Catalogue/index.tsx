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
  const logs = getFilteredLogs(null, "auction");

  return (
    <section className={styles.main}>
      <h3>Catalogue</h3>
      <table>
        <thead>
          <tr>
            <th />
            <th>Lot</th>
            <th>Lot Id</th>
            <th># Bids</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {lots.map((lot) => (
            <CatalogueItem key={lot.artworkId} lot={lot} />
          ))}
        </tbody>
      </table>
      <div className={styles.log}>
        <ActivityLog title="Gallery Item Audit Log" inline={true} logs={logs} />
      </div>
    </section>
  );
}

export default Catalogue;
