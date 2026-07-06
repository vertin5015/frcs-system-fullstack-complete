生成 SVG/PNG（需 JDK；已将 plantuml.jar 放入本目录）：

  generate.bat

或手动：

  java -jar plantuml.jar -charset UTF-8 -tsvg -tpng -o out *.puml

导出后已将主图 SVG 复制到上级目录 thesis_assets\，与《论文初稿_全文_同步.doc》中 img 路径一致。

检查说明（2026-05）：旧版 thesis_assets 根目录下若干 SVG 为手工/Ascii 示意；现已用本目录 PlantUML 重新生成并覆盖 fig3_1、fig3_2、fig3_3、fig4_1、fig4_2 及 fig_optional_sequence、fig_optional_er（对应论文图4.3、4.4）。
