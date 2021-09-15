import {
  Button,
  DateTimeInput,
  Select,
  TextInput,
  Option,
  Loader,
} from "@r3/r3-tooling-design-system";
import { GalleryLot, Participant } from "@Models";
import styles from "./styles.module.scss";
import { useContext, useState } from "react";
import Modal from "react-modal";
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
  const [expiryTime, setExpiryTime] = useState("");
  const [bidPosted, setBidPosted] = useState(false);

  if (!user || !selectedArtwork) return null;

  const bidRecognised = !!usersBid(selectedArtwork, user);
  const tokens = getTokensByUser(user);

  function handleBid() {
    if (user && selectedArtwork) {
      setBidPosted(true);
      postBid({
        bidderParty: user.x500,
        artworkId: selectedArtwork.artworkId,
        amount,
        currency: tokenType,
        expiryDate: expiryTime,
      });
    }
  }

  function handleCancel() {
    setAmount("");
    setTokenType("");
    setExpiryTime("");
    setBidPosted(false);
    onClose();
  }
  return selectedArtwork ? (
    <Modal
      isOpen={open}
      contentLabel="Place Bid"
      className={styles.modal}
      overlayClassName={styles.overlay}
      ariaHideApp={false}
    >
      {bidPosted && !bidRecognised && (
        <div className={styles.bidPosting}>
          <Loader size="small" />
        </div>
      )}
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
          {tokens.map((token) => (
            <Option key={token.currencyCode} value={token.currencyCode}>
              {token.currencyCode}
            </Option>
          ))}
        </Select>
      </div>
      <DateTimeInput
        className={styles.input}
        label="Expiry time"
        onChange={(time: any) => {
          setExpiryTime(time[0].toISOString());
        }}
        value={null}
      />
      <div className={styles.ctas}>
        <Button size="small" variant="secondary" onClick={() => handleCancel()}>
          Cancel
        </Button>
        <Button size="small" variant="primary" onClick={handleBid}>
          Place Bid
        </Button>
      </div>
    </Modal>
  ) : null;
}

export default GalleryBidModal;
