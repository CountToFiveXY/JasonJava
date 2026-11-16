# Macro files

Store Jitbit Macro Recorder `.mcr` files in this directory.

Use descriptive, stable names. These are source fragments consumed by the
classes in `jasonjava.macro`, for example:

```text
macrofiles/
├── reset.mcr
├── ad.mcr
└── SE/
```

Do not store credentials, machine-specific secrets, or generated logs here.

Generated, runnable macros are written to the ignored `generated-macros/`
directory. Run the `jasonjava.macro.RefillAds` main class to create
`generated-macros/refill-ads.mcr` from eight copies of `ad.mcr`.

See [the Windows setup guide](../docs/windows-setup.md) for the required names,
command-line launchers, and end-to-end installation instructions.
