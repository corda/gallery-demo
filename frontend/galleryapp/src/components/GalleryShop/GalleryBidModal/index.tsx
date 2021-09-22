import { Button, Select, TextInput, Option, Loader, Modal } from "@r3/r3-tooling-design-system";
import { GalleryLot, Participant } from "@Models";
import styles from "./styles.module.scss";
import { useContext, useState } from "react";
import { TokensContext } from "@Context/tokens";
import { postBid } from "@Api";
import { usersBid } from "@Utils";

interface Props {
  open: boolean;
  onClose: () => void;
  selectedArtwork: GalleryLot | null;
  user: Participant | null;
}

function GalleryBidModal({ open, onClose, selectedArtwork, user }: Props) {
  const { getTokensByUser } = useContext(TokensContext);
  const [amount, setAmount] = useState("");
  const [tokenType, setTokenType] = useState("");
  const [bidPosted, setBidPosted] = useState(false);

  if (!user || !selectedArtwork) return null;

  const biddersBid = usersBid(selectedArtwork, user);
  const tokens = getTokensByUser(user);

  function handleBid() {
    if (user && selectedArtwork) {
      setBidPosted(true);
      postBid({
        bidderParty: user.x500,
        artworkId: selectedArtwork.artworkId,
        amount,
        currency: tokenType,
      });
    }
  }

  function handleCancel() {
    setAmount("");
    setTokenType("");
    setBidPosted(false);
    onClose();
  }

  return selectedArtwork ? (
    <Modal onClose={handleCancel} size="small" title="" withBackdrop open={open}>
      {bidPosted && !biddersBid && (
        <div className={styles.bidPosting}>
          <Loader size="small" />
        </div>
      )}
      {!biddersBid && (
        <>
          <h4>Place Bid</h4>
          <div className={styles.amountInputs}>
            <TextInput
              className={styles.input}
              label="Bid amount"
              onChange={(event): void => {
                setAmount(event.target.value);
              }}
              value={amount}
              type="number"
            />
            <Select
              label="Asset"
              onChange={(event) => {
                setTokenType(event.target.value);
              }}
              value={tokenType}
              className={styles.input}
            >
              <Option key="select-token" value="select token">
                Select Token
              </Option>
              {tokens.map((token) => (
                <Option key={token.currencyCode} value={token.currencyCode}>
                  {token.currencyCode}
                </Option>
              ))}
            </Select>
          </div>
          <div className={styles.ctas}>
            <Button size="small" variant="secondary" onClick={() => handleCancel()}>
              Cancel
            </Button>
            <Button size="small" variant="primary" onClick={handleBid}>
              Place Bid
            </Button>
          </div>
        </>
      )}

      {!!biddersBid && (
        <div className="text-center">
          <h4>Bid placed successfully</h4>
          <ul className={styles.bidDetails}>
            {Object.entries(biddersBid).map((entry) => {
              const [key, value] = entry;

              return (
                <li key={key}>
                  <span className={styles.bidDetailsKey}>{key}:</span>
                  <span className={styles.bidDetailsValue}>{value.toString()}</span>
                </li>
              );
            })}
          </ul>
          <div className="justify-center flex">
            <Button size="small" variant="primary" onClick={() => handleCancel()}>
              Close
            </Button>
          </div>
        </div>
      )}
    </Modal>
  ) : null;
}

export default GalleryBidModal;
