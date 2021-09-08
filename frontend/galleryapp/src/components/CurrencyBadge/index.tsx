import { Badge } from "@r3/r3-tooling-design-system";
import config from "@Config";
import { FC } from "react";

interface Props {
  currencyCode: string;
}

const CurrencyBadge: FC<Props> = ({ currencyCode, children }) => {
  const currencyColor = config.networks[currencyCode].color;

  return (
    <Badge customColour={currencyColor} variant="gray">
      {children}
    </Badge>
  );
};

export default CurrencyBadge;
