import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class DictationA4Ultimate extends JFrame {
    private JTextArea inputArea;
    private JTextField countField;
    private File selectedFile = null;
    private JLabel fileLabel;

    private final String FONT_PATH = "/System/Library/Fonts/STHeiti Light.ttc";

    public DictationA4Ultimate() {
        setTitle("A4 拼音默写生成器 - 专业版");
        setSize(650, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);

        // --- UI 布局 ---
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton selectFileBtn = new JButton("选择词库文件 (.txt)");
        fileLabel = new JLabel("未选择文件（将使用下方文本框）");
        fileLabel.setForeground(Color.GRAY);
        fileRow.add(selectFileBtn);
        fileRow.add(fileLabel);

        JPanel configRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        configRow.add(new JLabel("随机抽取数量:"));
        countField = new JTextField("20", 5);
        configRow.add(countField);
        topPanel.add(fileRow);
        topPanel.add(configRow);
        add(topPanel, BorderLayout.NORTH);

        inputArea = new JTextArea("输入词语，空格分隔。例如：清风 拂面 柳绿 花红");
        inputArea.setLineWrap(true);
        inputArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(" 手动输入/预览区 "),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        add(new JScrollPane(inputArea), BorderLayout.CENTER);

        JButton genBtn = new JButton("生成 PDF 默写本");
        styleBlueButton(genBtn);
        JPanel btnPanel = new JPanel(new BorderLayout());
        btnPanel.setBorder(new EmptyBorder(10, 20, 20, 20));
        btnPanel.add(genBtn, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        // --- 逻辑绑定 ---
        selectFileBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedFile = chooser.getSelectedFile();
                fileLabel.setText("已选: " + selectedFile.getName());
                fileLabel.setForeground(new Color(0, 128, 0));
                inputArea.setEnabled(false);
            }
        });

        genBtn.addActionListener(e -> startGenerateProcess());
    }

    private void styleBlueButton(JButton btn) {
        btn.setFont(new Font("Heiti SC", Font.BOLD, 16));
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setBackground(new Color(0, 122, 255));
        btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(0, 50));
    }

    private void startGenerateProcess() {
        try {
            List<String> words = getTargetWords();
            if (words.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入或选择词语内容！");
                return;
            }

            // 让用户选择保存路径
            JFileChooser saveChooser = new JFileChooser();
            saveChooser.setSelectedFile(new File("默写本_" + System.currentTimeMillis() / 1000 + ".pdf"));
            File targetFile;

            if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                targetFile = saveChooser.getSelectedFile();
            } else {
                // 用户取消，则使用默认位置（项目根目录）
                targetFile = new File("Default_Dictation.pdf");
            }

            executeExport(words, targetFile);
            showSuccessOption(targetFile);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "生成失败: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private List<String> getTargetWords() throws Exception {
        if (selectedFile != null) {
            String content = Files.readString(selectedFile.toPath());
            List<String> list = new ArrayList<>(Arrays.asList(content.split("\\s+")));
            list.removeIf(String::isEmpty);
            Collections.shuffle(list);
            int count = Integer.parseInt(countField.getText().trim());
            return list.subList(0, Math.min(count, list.size()));
        }
        return Arrays.asList(inputArea.getText().trim().split("\\s+"));
    }

    private void executeExport(List<String> words, File targetFile) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            // 加载字体 (PDFBox 2.x 兼容逻辑)
            AtomicReference<PDType0Font> fontRef = new AtomicReference<>();
            File fontFile = new File(FONT_PATH);
            if (!fontFile.exists()) fontFile = new File("/System/Library/Fonts/PingFang.ttc");
            new TrueTypeCollection(fontFile).processAllFonts(ttf -> {
                try { if (fontRef.get() == null) fontRef.set(PDType0Font.load(doc, ttf, true)); } catch (Exception ignored) {}
            });
            PDType0Font font = fontRef.get();

            HanyuPinyinOutputFormat pyFormat = new HanyuPinyinOutputFormat();
            pyFormat.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
            pyFormat.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);

            int pageIdx = 1;
            float x = 50, y = 710; // 起始坐标
            PDPage page = createPage(doc, font, pageIdx++);
            PDPageContentStream canvas = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true);

            for (String word : words) {
                if (word.trim().isEmpty()) continue;
                float wWidth = word.length() * 50;
                if (x + wWidth > 545) { x = 50; y -= 95; }
                if (y < 80) {
                    canvas.close();
                    page = createPage(doc, font, pageIdx++);
                    canvas = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true);
                    x = 50; y = 710;
                }

                for (char c : word.toCharArray()) {
                    String[] py = PinyinHelper.toHanyuPinyinStringArray(c, pyFormat);
                    String pStr = (py != null && py.length > 0) ? py[0] : "";

                    canvas.beginText();
                    canvas.setFont(font, 12);
                    float pyW = font.getStringWidth(pStr) / 1000 * 12;
                    canvas.newLineAtOffset(x + (45 - pyW)/2, y);
                    canvas.showText(pStr);
                    canvas.endText();

                    canvas.setStrokingColor(Color.BLACK);
                    canvas.addRect(x, y - 53, 45, 45);
                    canvas.stroke();

                    canvas.setLineDashPattern(new float[]{2, 2}, 0);
                    canvas.setStrokingColor(210/255f, 210/255f, 210/255f);
                    canvas.moveTo(x + 22.5f, y - 53); canvas.lineTo(x + 22.5f, y - 8);
                    canvas.moveTo(x, y - 30.5f); canvas.lineTo(x + 45, y - 30.5f);
                    canvas.stroke();
                    canvas.setLineDashPattern(new float[]{}, 0);
                    x += 50;
                }
                x += 20;
            }
            canvas.close();
            doc.save(targetFile);
        }
    }

    private void showSuccessOption(File file) {
        String msg = "PDF 已生成至：\n" + file.getAbsolutePath();
        Object[] options = {"打开文件", "打开所在的文件夹", "确定"};
        int choice = JOptionPane.showOptionDialog(this, msg, "生成成功",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[2]);

        try {
            if (choice == 0) Desktop.getDesktop().open(file); // 打开 PDF
            else if (choice == 1) Desktop.getDesktop().open(file.getParentFile()); // 打开文件夹
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "无法打开路径: " + ex.getMessage());
        }
    }

    private PDPage createPage(PDDocument doc, PDType0Font font, int no) throws Exception {
        PDPage p = new PDPage(PDRectangle.A4);
        doc.addPage(p);
        try (PDPageContentStream s = new PDPageContentStream(doc, p)) {
            s.beginText(); s.setFont(font, 18);
            s.newLineAtOffset(220, 800); s.showText("语文词语默写本"); s.endText();
            s.beginText(); s.setFont(font, 11);
            s.newLineAtOffset(50, 770);
            s.showText("姓名：__________ 日期：20__年__月__日    (第 " + no + " 页)");
            s.endText();
            s.setLineWidth(0.5f); s.moveTo(50, 760); s.lineTo(545, 760); s.stroke();
        }
        return p;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DictationA4Ultimate().setVisible(true));
    }
}
