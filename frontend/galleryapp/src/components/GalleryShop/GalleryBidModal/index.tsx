import { Button, Modal, TextInput } from "@r3/r3-tooling-design-system";
import { GalleryLot } from "../../../models";
import styles from "./styles.module.scss";

interface Props {
  open: boolean;
  onClose: () => void;
  selectedBid: GalleryLot | null;
}

function GalleryBidModal({ open, onClose, selectedBid }: Props) {
  return selectedBid ? (
    <Modal closeOnOutsideClick onClose={onClose} size="small" title="" withBackdrop open={open}>
      <h4>Place Bid</h4>
      <TextInput className={styles.input} label="Bid amount" onChange={() => {}} value="" />
      <TextInput className={styles.input} label="Expiry time" onChange={() => {}} value="" />
      <div className={styles.ctas}>
        <Button size="small" variant="secondary" onClick={() => onClose()}>
          Cancel
        </Button>
        <Button size="small" variant="primary">
          Place Bid
        </Button>
      </div>
    </Modal>
  ) : null;
}

export default GalleryBidModal;
