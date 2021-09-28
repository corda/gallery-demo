var __assign = (this && this.__assign) || function () {
    __assign = Object.assign || function(t) {
        for (var s, i = 1, n = arguments.length; i < n; i++) {
            s = arguments[i];
            for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
                t[p] = s[p];
        }
        return t;
    };
    return __assign.apply(this, arguments);
};
var __rest = (this && this.__rest) || function (s, e) {
    var t = {};
    for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p) && e.indexOf(p) < 0)
        t[p] = s[p];
    if (s != null && typeof Object.getOwnPropertySymbols === "function")
        for (var i = 0, p = Object.getOwnPropertySymbols(s); i < p.length; i++) {
            if (e.indexOf(p[i]) < 0 && Object.prototype.propertyIsEnumerable.call(s, p[i]))
                t[p[i]] = s[p[i]];
        }
    return t;
};
import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState, isValidElement, Children, } from 'react';
import uniqId from 'uniqid';
import './TopNavBar.scss';
var previewConfig = {
    name: 'TopNavBar',
    defaultState: {
        background: 'dark',
        centerPos: 'end',
        componentProps: {
            logo: '/static/media/logo--r3.13d19744.svg',
            title: 'tooling design system',
            logoWidth: '33px',
            logoAlt: 'R3 | DLT & Blockchain Software Development Company',
        },
    },
    centerPos: {
        type: 'radio',
        values: ['start', 'center', 'end'],
    },
};
var TopNavBar = function (_a) {
    var _b = _a.className, className = _b === void 0 ? '' : _b, logo = _a.logo, logoWidth = _a.logoWidth, logoHeight = _a.logoHeight, logoAlt = _a.logoAlt, title = _a.title, center = _a.center, centerPos = _a.centerPos, right = _a.right, id = _a.id, children = _a.children, otherProps = __rest(_a, ["className", "logo", "logoWidth", "logoHeight", "logoAlt", "title", "center", "centerPos", "right", "id", "children"]);
    var navId = useState(id ? id : uniqId())[0];
    var style = 'ml-4 uppercase text-xs cursor-pointer font-bold hover:text-blue text-medium-light-gray focus:shadow-large-blur-dark';
    var rightNavItems = [];
    Children.forEach(right === null || right === void 0 ? void 0 : right.props.children, function (child, i) {
        if (isValidElement(child)) {
            rightNavItems.push(_jsx("div", __assign({ className: "header-nav-item " + style }, { children: child }), uniqId()));
        }
    });
    var centerNavItems = [];
    Children.forEach(center === null || center === void 0 ? void 0 : center.props.children, function (child, i) {
        if (isValidElement(child)) {
            centerNavItems.push(_jsx("div", __assign({ className: "header-nav-item " + style }, { children: child }), uniqId()));
        }
    });
    return (_jsxs("nav", __assign({ id: navId, className: "header-nav flex items-center bg-medium-light-gray-100 w-full h-16 px-10 py-1 " + className }, otherProps, { children: [_jsxs("div", __assign({ className: "flex items-center pr-8" }, { children: [_jsx("img", { src: logo, width: logoWidth, height: logoHeight, alt: logoAlt || logo, className: "mr-4 max-h-full" }, void 0),
                    _jsx("h1", __assign({ className: "flex items-center leading-snug tracking-larger font-bold text-medium-light-gray-600 text-xs uppercase" }, { children: title }), void 0)] }), void 0),
            center ? (_jsx("div", __assign({ className: (center && right ? 'pr-8' : '') + " " + (centerPos ? "justify-" + centerPos : 'justify-end') + " flex flex-1 item-center" }, { children: centerNavItems }), void 0)) : (''),
            right ? (_jsx("div", __assign({ className: (center && right ? 'flex-0' : 'flex-1') + " flex item-center justify-end" }, { children: rightNavItems }), void 0)) : ('')] }), void 0));
};
export default TopNavBar;
export { previewConfig };
//# sourceMappingURL=TopNavBar.js.map