![VInject](https://socialify.git.ci/WonderfulPanic/VInject/image?description=1&font=Inter&language=1&name=1&owner=1&pattern=Circuit%20Board&theme=Auto)

VInject is a wrapper for Minecraft server proxy [Velocity](https://github.com/PaperMC/Velocity)

It allows plugin developers to modify Velocity's code at runtime, making it possible to extend the proxy's core functionality

## 🧩 How to install

- Put `vinject-injector-VERSION.jar` in the same folder as `velocity-VERSION.jar`

- Add VInject's jar name before Velocity's jar name in command line:

  ```diff
  - java -jar velocity-VERSION.jar
  + java -jar vinject-injector-VERSION.jar velocity-VERSION.jar
  ```

- You should see a message:
  
  `[VInject] VInject version VERSION`

## 🎲 How VInject works

VInject changes class loading process in plugins and Velocity. It modifies loaded classes using the code provided by plugins

![class loading hierarchy](https://github.com/user-attachments/assets/6e76301f-8d11-44c3-be07-bea177eb3c27)

# 🛠️ Developing

# 🐞 Debugging

- `-Dvinject.debug=true` Outputs additional info
- `-Dvinject.export=true` Exports generated class-files into `vinject-export` directory. You can decompile those class-files using for example [Bytecode-Viewer](https://github.com/Konloch/bytecode-viewer) to analyze generated code
- `-Dvinject.forceload=true` Loads all classes affected by VInject to verify their integrity. Useful for testing if your classes are correct
