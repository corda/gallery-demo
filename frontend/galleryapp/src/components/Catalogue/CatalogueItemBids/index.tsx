import styles from "./styles.module.scss";
import { Bid, RouterParams } from "@Models";
import { Badge, Button, Loader } from "@r3/r3-tooling-design-system";
import { useContext, useState } from "react";
import { postBidAcceptance } from "@Api";
import { useParams } from "react-router-dom";
import { UsersContext } from "@Context/users";

interface Props {
  bids: Bid[];
  lotId: string;
  open: boolean;
}

function getStatus(bidAccepted: boolean, biddingOpen: boolean) {
  if (bidAccepted) {
    return <Badge variant="green">Accepted</Badge>;
  }
  if (!biddingOpen && !bidAccepted) {
    return <Badge variant="red">Unsuccessful</Badge>;
  }

  return <Badge variant="gray">Open</Badge>;
}

function CatalogueItemBids({ bids, open, lotId }: Props) {
  const { getUser } = useContext(UsersContext);
  const [bidAccepted, setBidAccepted] = useState(false);
  const pendingState = bidAccepted && open;
  const { id } = useParams<RouterParams>();
  const user = getUser(id);

  function handleBidAcceptance(bid: Bid) {
    if (user) {
      setBidAccepted(true);
      postBidAcceptance({
        bidderParty: bid.bidderDisplayName,
        artworkId: lotId,
        currency: bid.currencyCode
      });
    }
  }

  return (
    <tr className={styles.main}>
      <td colSpan={6} className={styles.bidsRow}>
        {pendingState ? (
          <div className={styles.spinner}>
            <Loader size="small" />
          </div>
        ) : null}

        <div className={pendingState ? styles.bidPending : ""}>
          {!bids.length ? (
            <h6>No bids have been placed yet.</h6>
          ) : (
            <table>
              <thead className={styles.tableHead}>
                <tr>
                  <th>Patron</th>
                  <th>Bid value</th>
                  <th>Network</th>
                  <th>Status</th>
                  {open ? <th /> : null}
                </tr>
              </thead>
              <tbody>
                {bids.map((bid) => (
                  <tr
                    key={bid.cordaReference}
                    className={!bid.accepted && !open ? styles.bidRejected : ""}
                  >
                    <td>{bid.bidderDisplayName}</td>
                    <td>
                      {bid.accepted ? (
                        <Badge variant="green">
                          {bid.amount} {bid.currencyCode}
                        </Badge>
                      ) : (
                        `${bid.amount} ${bid.currencyCode}`
                      )}
                    </td>
                    <td>
                      <Badge variant="gray">{bid.notary}</Badge>
                    </td>
                    <td>{getStatus(bid.accepted, open)}</td>
                    {open ? (
                      <td>
                        <Button
                          size="small"
                          variant="tertiary"
                          onClick={() => handleBidAcceptance(bid)}
                        >
                          Accept
                        </Button>
                      </td>
                    ) : null}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </td>
    </tr>
  );
}

export default CatalogueItemBids;
