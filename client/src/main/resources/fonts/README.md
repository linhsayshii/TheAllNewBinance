Place project fonts in this folder so JavaFX can load them from CSS.

Expected file names (used in css/base/fonts.css):
- InriaSerif-Regular.ttf
- InriaSerif-Bold.ttf
- Montserrat-Regular.ttf
- Montserrat-SemiBold.ttf
- Montserrat-Bold.ttf

If these files are missing, JavaFX will fall back to local installed fonts.
You can still switch all serif/sans usage centrally in css/base/fonts.css via:
- -font-family-serif
- -font-family-sans
