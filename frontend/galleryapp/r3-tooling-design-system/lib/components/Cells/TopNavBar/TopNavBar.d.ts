import React, { ReactElement } from 'react';
import './TopNavBar.scss';
declare const previewConfig: {
    name: string;
    defaultState: {
        background: string;
        centerPos: string;
        componentProps: {
            logo: string;
            title: string;
            logoWidth: string;
            logoAlt: string;
        };
    };
    centerPos: {
        type: string;
        values: string[];
    };
};
interface Props {
    className?: string;
    logo?: string;
    logoWidth?: string;
    logoHeight?: string;
    logoAlt?: string;
    title?: string;
    center?: ReactElement;
    centerPos?: 'start' | 'center' | 'end';
    right?: ReactElement;
    id?: string;
    [x: string]: any;
}
declare const TopNavBar: React.FC<Props>;
export default TopNavBar;
export { previewConfig };
