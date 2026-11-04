# Nebula sobre MojoLauncher

## Decisión de arquitectura

Nebula se construirá como una personalización profunda de MojoLauncher. MojoLauncher será la aplicación anfitriona y aportará el runtime Java, los procesos de juego, los instaladores y las bibliotecas nativas. Nebula aportará la identidad, la experiencia de usuario y sus funciones de catálogo, perfiles y configuración.

No se mantendrá el diseño actual de dos aplicaciones que se abren mutuamente. El resultado debe ser un único APK instalable.

## Qué se conserva

- Runtime Java y gestión de procesos de juego de MojoLauncher.
- Bibliotecas JNI y componentes nativos upstream.
- Instaladores y utilidades de ejecución compatibles con la licencia y el objetivo del proyecto.
- Funciones Nebula existentes: catálogo Modrinth, perfiles locales, selección de instalación y configuración, adaptadas a los servicios reales del motor.

## Qué cambia

- La actividad principal y la navegación serán las de Nebula, integradas en la aplicación anfitriona.
- Nombre, iconos, tema, textos y recursos de marca pasarán a ser Nebula.
- Se unificarán el identificador de paquete, manifiesto, permisos, proveedores, servicios y autoridades de almacenamiento.
- Se eliminará la lógica que busca o abre el paquete MojoLauncher como aplicación externa.
- Los ajustes de versión y cargador deberán convertirse en parámetros reales del instalador/runtime, no quedarse guardados solo en preferencias.

## Secuencia de implementación

1. **Base reproducible:** mantener fijado el commit upstream y sus submódulos; escoger una vía mantenible para los parches (fork upstream o código vendorizado). No depender de modificaciones temporales en GitHub Actions.
2. **Aplicación anfitriona:** adaptar el módulo de MojoLauncher para usar el identificador de paquete Nebula y la marca Nebula, preservando componentes técnicos y autoridades internas.
3. **Interfaz:** trasladar las pantallas Nebula al módulo anfitrión y convertirlas en la navegación principal. Conservar pantallas técnicas del motor donde sean necesarias.
4. **Servicios:** conectar perfiles, catálogo e instaladores Nebula con las APIs reales del motor. Eliminar mensajes que indiquen instalar un segundo APK.
5. **Calidad:** revisar compilación, permisos Android modernos, almacenamiento, ciclo de vida, rendimiento y consumo de memoria.
6. **Validación:** compilar un solo APK; verificar instalación y apertura; instalar y arrancar Vanilla 1.20.1; después integrar y probar Forge 47.4.5.

## Criterios de aceptación

- Un solo APK y un solo icono de aplicación.
- No requiere instalar MojoLauncher por separado.
- La interfaz y la marca visibles son Nebula.
- Vanilla 1.20.1 se instala y arranca desde Nebula.
- Los errores se registran y pueden diagnosticarse; no se ocultan con estados falsos de éxito.
- Forge 47.4.5 se aborda después de validar Vanilla.

## Estado inicial

El código actual todavía contiene dos módulos Android de tipo aplicación y la actividad principal de Nebula intenta abrir MojoLauncher como paquete externo. Los builds independientes existentes no prueban que exista un APK unificado ni que Minecraft arranque. Esta documentación define el destino; cada criterio se marcará como completado solo después de una validación reproducible.