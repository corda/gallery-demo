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
import './Loader.scss';
var previewConfig = {
    name: 'Loader',
    defaultState: {
        text: false,
        componentProps: {
            text: false,
        },
    },
    modifiers: {
        type: 'checkbox',
        options: [
            {
                name: 'text',
                properties: {
                    text: 'loading...',
                },
            },
        ],
    },
};
var Loader = function (_a) {
    var text = _a.text, otherProps = __rest(_a, ["text"]);
    return (_jsxs("div", __assign({ className: "m-auto flex flex-col items-center" }, { children: [_jsx("div", __assign({ className: "loader relative rounded-full m-auto" }, otherProps, { children: ' ' }), void 0),
            text && (_jsx("span", __assign({ className: "pt-4 text-xs text-dark-gray-300 font-bold uppercase tracking-large" }, { children: text }), void 0))] }), void 0));
};
export default Loader;
export { previewConfig };
//# sourceMappingURL=Loader.js.map