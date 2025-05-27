module.exports = {
    presets: [
        ['@babel/preset-env', {
            targets: {
                browsers: ['last 2 versions', 'not dead', '> 0.25%']
            },
            useBuiltIns: 'entry',
            corejs: 3
        }]
    ],
    plugins: [
        '@babel/plugin-proposal-optional-chaining',
        '@babel/plugin-proposal-nullish-coalescing-operator',
        // Zusätzliche Plugins für bessere Kompatibilität
        '@babel/plugin-proposal-class-properties',
        '@babel/plugin-syntax-dynamic-import'
    ]
};