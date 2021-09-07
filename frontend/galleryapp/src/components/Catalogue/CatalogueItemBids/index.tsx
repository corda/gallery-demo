import styles from "./styles.module.scss";
import { Bid } from "../../../models";
import ActivityLog from "@Components/ActivityLog";
import { Button } from "@r3/r3-tooling-design-system";

interface Props {
  bids: Bid[];
}

function CatalogueItemBids({ bids }: Props) {
  return (
    <tr className={styles.main}>
      <td colSpan={6} className={styles.bidsRow}>
        <div>
          <table>
            <thead className={styles.tableHead}>
              <tr>
                <th>Patron</th>
                <th>Bid value</th>
                <th>Network</th>
                <th>Expires in</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {bids.map((bid) => (
                <tr>
                  <td>{bid.patronDisplayName}</td>
                  <td>
                    {bid.value} {bid.currencySymbol}
                  </td>
                  <td>{bid.network}</td>
                  <td>{bid.expiryDate}</td>
                  <td>{bid.status}</td>
                  <td>
                    <Button size="small" variant="tertiary">
                      Accept
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className={styles.log}>
            <ActivityLog title="Gallery Item Audit Log" small={true} />
          </div>
        </div>
      </td>
    </tr>
  );
}

export default CatalogueItemBids;
