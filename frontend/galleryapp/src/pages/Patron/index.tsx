import { Column, Container, Row } from "@r3/r3-tooling-design-system";
import Wallets from "@Components/Wallets";
import { mockWallets } from "../../mockData";
import GalleryShop from "@Components/GalleryShop";
import Collection from "@Components/Collection";

function Patron() {
  return (
    <div>
      <Container>
        <Row>
          <Column cols={8}>
            <GalleryShop />
          </Column>
          <Column cols={4}>
            <Wallets wallets={mockWallets} />
            <Collection />
          </Column>
        </Row>
      </Container>
    </div>
  );
}

export default Patron;
