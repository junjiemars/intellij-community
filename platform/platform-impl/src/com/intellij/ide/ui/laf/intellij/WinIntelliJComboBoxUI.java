/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.ui.laf.intellij;

import com.intellij.ide.ui.laf.darcula.DarculaUIUtil;
import com.intellij.ide.ui.laf.darcula.ui.DarculaComboBoxUI;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.ui.Gray;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicArrowButton;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class WinIntelliJComboBoxUI extends DarculaComboBoxUI {
  public WinIntelliJComboBoxUI(JComboBox comboBox) {
    super(comboBox);
  }


  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "UnusedDeclaration"})
  public static ComponentUI createUI(JComponent c) {
    return new WinIntelliJComboBoxUI((JComboBox)c);
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    final int w = c.getWidth();
    final int h = c.getHeight();

    if (c.isOpaque()) {
      final Container parent = c.getParent();
      if (parent != null) {
        g.setColor(isTableCellEditor(c) && editor != null ? editor.getBackground() : parent.getBackground());
        g.fillRect(0, 0, c.getWidth(), c.getHeight());
      }
    }
    Rectangle r = rectangleForCurrentValue();
    g.setColor(getComboBackground());
    g.fillRect(JBUI.scale(1), JBUI.scale(1), w-2*JBUI.scale(1), h-2*JBUI.scale(1));
    if (!isTableCellEditor(c)) {
      paintBorder(c, g, 0, 0, w, h);
      hasFocus = comboBox.hasFocus();
      paintCurrentValueBackground(g, r, hasFocus);
    }
    paintCurrentValue(g, r, hasFocus);
  }

  protected Color getComboBackground() {
    if (!comboBox.isEnabled() && !comboBox.isEditable()) {
      return UIManager.getColor("ComboBox.disabledBackground");
    }
    if (comboBox.isEditable()) {
      final ComboBoxEditor editor = comboBox.getEditor();
      if (editor != null && editor.getEditorComponent() != null) {
        return editor.getEditorComponent().getBackground();
      } else {
        return Gray.xFF;
      }
    }
    return comboBox.getBackground();
  }

  @Override
  protected JButton createArrowButton() {
    final JButton button = new BasicArrowButton(SwingConstants.SOUTH) {
      @Override
      public Dimension getPreferredSize() {
        return JBUI.size(14, 10);
      }

      @Override
      public void paint(Graphics g) {
        g.setColor(getComboBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        IconUtil.paintInCenterOf(this, g, MacIntelliJIconCache.getIcon("comboDropTriangle", false, false, isEnabled()));
      }
    };
    button.setOpaque(false);
    button.setBorder(JBUI.Borders.empty());
    return button;
  }

  @Override
  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    if (comboBox == null || arrowButton == null) {
      return; //NPE on LaF change
    }

    Graphics2D g2 = (Graphics2D)g.create();
    try {
      hasFocus = false;
      checkFocus();
      Object eop = ((JComponent)c).getClientProperty("JComponent.error.outline");
      if (Registry.is("ide.inplace.errors.outline") && Boolean.parseBoolean(String.valueOf(eop))) {
        g2.translate(x, y);
        DarculaUIUtil.paintErrorBorder(g2, width, height, 0, true, hasFocus);
      } else if (hasFocus) {
        g2.setColor(UIManager.getColor("ComboBox.activeBorderColor"));
        g2.setStroke(new BasicStroke(JBUI.scale(2f)));
      } else {
        g2.setColor(UIManager.getColor("ComboBox.borderColor"));
      }

      g2.translate(x, y);
      g2.drawRect(JBUI.scale(1), JBUI.scale(1), width-2*JBUI.scale(1), height-2*JBUI.scale(1));
    } finally {
      g2.dispose();
    }
  }

  @Override
  protected Insets getInsets() {
    return JBUI.insets(4, 5).asUIResource();
  }
}
