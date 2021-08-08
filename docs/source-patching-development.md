# Source Patching Development

This document explains the purpose of source patching development and How to use source patching development.

## The purpose of source patching development

If the mod you want to modify doesn't have public source code or redistributing sources is not allowed, the only way to
modify sources is applying patches.

It's not good to use source patching development for source published mods. Please use forking mod.

## How to use source patching development by this plugin

Before do those steps, please configure `com.anatawa12.mod-patching.source` gradle plugin. See README for configuring
information.

### Step 1: run ``preparePatchingEnvironment`` task

Please run `preparePatchingEnvironment` gradle task added by this plugin. Running this prepares mods will be modified.

### Step 2: run ``./pm.apply-patches``

To apply existing patches, please execute ``./pm.apply-patches``.

### Step 3: run ``./pm.add-modify``

If you want to modify classes already existing in patched java sources, this step is not required. If you want to add
classes to be modified, run ``./pm.add-modify`` and select class from list. If your terminal emulator supports, mouse
click and scroll can be used to select java files.

### Step 3: modify java files

Please edit the files you want to modify.

### Step 4: run ``./pm.create-diff``

Before commit, please run ``./pm.create-diff`` to generate patch files. Without this, your changes will not be added to
git repository.

