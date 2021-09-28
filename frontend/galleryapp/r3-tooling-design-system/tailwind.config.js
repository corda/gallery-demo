module.exports = {
  purge: [],
  theme: {
    screens: {
      'sm': {'min': '720px', 'max': '839px'},
      'md': {'min': '840px', 'max': '1365px'},
      'lg': {'min': '1366px'},
    },
    colors: {
      'white': {
        default: '#ffffff',
      },
      'black': {
        default: '#000000',
      },
      'transparent': {
        default: 'transparent',
      },
      'blue': {
        default: '#5e39e9',
        100: '#dfd7fb',
        200: '#bfb0f6',
        300: '#9e88f2',
        400: '#7e61ed',
        500: '#472baf',
        600: '#2f1d75',
      },
      'dark-gray': {
        default: '#1d2343',
        100: '#d2d3d9',
        200: '#a5a7b4',
        300: '#777b8e',
        400: '#4a4f69',
        500: '#161a32',
        600: '#0f1222',
      },
      'medium-dark-gray': {
        default: '#3b4264',
        100: '#d8d9e0',
        200: '#b1b3c1',
        300: '#898ea2',
        400: '#626883',
        500: '#2c324b',
        550: '#282e4c',
        600: '#1e2132',
      },
      'medium-light-gray': {
        default: '#919ebd',
        100: '#e9ecf2',
        200: '#d3d8e5',
        300: '#bdc5d7',
        400: '#a7b1ca',
        500: '#6d778e',
        600: '#494f5f',
      },
      'light-gray': {
        default: '#f8f8f8',
        100: '#fefefe',
        200: '#fcfcfc',
        300: '#fbfbfb',
        400: '#f9f9f9',
        500: '#bababa',
        600: '#7c7c7c',
      },
      green: {
        default: '#00a37e',
        100: '#ccede5',
        200: '#99dacb',
        300: '#66c8b2',
        400: '#33b598',
        500: '#007a5f',
        600: '#00523f',
      },
      yellow: {
        default: '#f1c40f',
        100: '#fcf3cf',
        200: '#f9e79f',
        300: '#f7dc6f',
        400: '#f4d03f',
        500: '#b5930b',
        600: '#796208',
      },
      red: {
        default: '#ec1d24',
        50: '#fde8e9',
        100: '#fbd2d3',
        200: '#f7a5a7',
        300: '#f4777c',
        400: '#f04a50',
        500: '#b1161b',
        600: '#760f12',
      },
    },
    extend: {
      fontFamily: {
        title: ['Poppins'],
        body: ['"PT Sans"'],
      },
      fontWeight: {
        bold: 600,
      },
      fontSize: {
        xl: '1.375rem',
        '3xl': '1.625rem',
        '5xl': '2.875rem',
      },
      letterSpacing: {
        smallest: '0.0275em',
        smaller: '0.0375em',
        small: '0.04375em',
        medium: '0.058em',
        large: '0.09em',
        larger: '0.12em',
      },
      boxShadow: {
        '0-0-8-dark-gray': '0px 0px 8px #1D234366',
        '0-0-7-dark-gray': '0px 0px 7px #1D234380',
        '0-0-7-red-600': '0px 0px 7px #760F12B3',
        '0-0-7-green-600': '0px 0px 7px #00523FB3',
        '0-0-12-blue': '0px 0px 12px #5E39E9',
        '0-0-12-white': '0px 0px 12px #FFFFFF',
        '0-0-12-medium-light-gray': '0px 0px 12px #919EBD',
        '0-0-12-yellow-600': '0px 0px 12px #796208',
        '0-0-12-red-600': '0px 0px 12px #760F12',
        '0-0-12-red-400': '0px 0px 12px #F04A50',
        '0-1-6-blue': '0px 1px 6px #5E39E966',
        '0-1-6-bluegray': '0px 1px 6px #46367080',
        '0-1-6-dark-gray': '0px 1px 6px #1D234340',
        '0-3-6-dark-gray': '0px 3px 6px #1D2343B3',
        '0-3-6-dark-gray-1': '0px 3px 6px #1D23431A',
        '0-3-6-yellow-600': '0px 3px 6px #796208CC',
        '0-3-6-red-600': '0px 3px 6px #760F1299',
        '0-3-6-bluegray': '0px 3px 6px #46367066',
      },
      minWidth: {
        '5': '1.25rem',
      },
      lineHeight: {
        '0': '0',
        normal: 'normal',
      },
      maxHeight: {
        '32': '8rem',
        '48': '12rem',
        '1/2': '50%',
      },
      maxWidth: {
        '1/3': '33%',
      },
      minHeight: {
        '6': '1.5rem',
        '10': '2.5rem',
        '12': '3rem',
      },
      width: {
        '15': '3.75rem',
        '28': '7rem',
        '128': '32rem',
      },
      height: {
        '4-1/2': '1.125rem',
        '5-1/2': '1.375rem',
        '7': '1.875rem',
        '128': '32rem',
        '1/2': '50%'
      },
      inset: {
        '-4': '-1rem',
        '-2': '-0.5rem',
        xxs: '0.3125rem',
        xs: '0.4375rem',
        sm: '0.875rem',
        '2': '0.5rem',
        '3': '0.75rem',
        '4': '1rem',
        '5': '1.25rem',
        '6': '1.5rem',
        md: '1.6875rem',
        '1/2': '50%',
        'full': '100%'
      },
      spacing: {
        '-1/2': '-50%'
      },
      borderRadius: {
        'xl': '1rem'
      }
    },
  },
  variants: {},
  plugins: [
    require('tailwind-css-variables') (
      {
        // modules
      },
      {
        // options
      }
    )
  ],
};
