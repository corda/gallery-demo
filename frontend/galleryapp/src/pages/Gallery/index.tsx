import { Column, Container, Row } from "@r3/r3-tooling-design-system";
import Catalogue from "@Components/Catalogue";
import Wallets from "@Components/Wallets";
import { mockGalleryLots, mockWallets } from "../../mockData";

function Gallery() {
  return (
    <div>
      <Container>
        <Row>
          <Column cols={8}>
            <Catalogue lots={mockGalleryLots} />
          </Column>
          <Column cols={4}>
            <Wallets wallets={mockWallets} />
          </Column>
        </Row>
      </Container>
    </div>
  );
}

export default Gallery;
