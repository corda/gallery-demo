import { Participant } from "@Models";
import styles from "./styles.module.scss";
import { DashboardItem } from "@r3/r3-tooling-design-system";
import { Link } from "react-router-dom";
import { getParticipantPath } from "@Helpers";

interface Props {
  participants: Participant[];
}

type Astronauts =
  | "AstronautFishing"
  | "AstronautFlight"
  | "AstronautHello"
  | "AstronautSittingOnPlanet"
  | "AstronautWave"
  | "AstronautWithFlag";

const pictograms: Astronauts[] = [
  "AstronautFishing",
  "AstronautFlight",
  "AstronautHello",
  "AstronautSittingOnPlanet",
  "AstronautWave",
  "AstronautWithFlag",
];

function ParticipantSelection({ participants }: Props) {
  return (
    <div className={styles.main}>
      {participants.map((participant, i) => (
        <Link key={participant.displayName} to={getParticipantPath(participant)}>
          <DashboardItem icon={pictograms[i]} withBackground className={styles.item}>
            {participant.displayName}
          </DashboardItem>
        </Link>
      ))}
    </div>
  );
}

export default ParticipantSelection;
