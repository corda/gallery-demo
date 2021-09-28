import React from 'react';
interface Props {
    size: 'large' | 'small';
    title: string;
    className?: string;
    open?: boolean;
    variant?: 'danger' | 'warning' | 'success';
    withBackdrop?: boolean;
    onClose: () => void;
    [x: string]: any;
}
declare const previewConfig: {
    name: string;
    trigger: boolean;
    triggerText: string;
    triggerFn: () => void;
    defaultState: {
        size: string;
        open: boolean;
        withBackdrop: boolean;
        componentProps: {
            size: string;
            title: string;
            open: boolean;
            onClose: () => void;
        };
    };
    modifiers: {
        type: string;
        options: ({
            name: string;
            properties: {
                withBackdrop: boolean;
                open?: undefined;
            };
        } | {
            name: string;
            properties: {
                open: boolean;
                withBackdrop?: undefined;
            };
        })[];
    };
    variant: {
        type: string;
        values: string[];
    };
    size: {
        type: string;
        values: string[];
    };
};
declare const Modal: React.FC<Props>;
export default Modal;
export { previewConfig };
