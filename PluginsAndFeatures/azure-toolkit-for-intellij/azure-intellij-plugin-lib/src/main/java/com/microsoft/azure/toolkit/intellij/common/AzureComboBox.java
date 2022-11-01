/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.cache.LRUStack;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.BadLocationException;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AzureComboBox<T> extends ComboBox<T> implements AzureFormInputComponent<T> {
    public static final String EMPTY_ITEM = StringUtils.EMPTY;
    private static final int DEBOUNCE_DELAY = 500;
    private final TailingDebouncer reloader;
    private AzureComboBoxEditor myEditor;
    private boolean valueNotSet = true;
    private boolean loading = false;
    protected Object value;
    protected boolean enabled = true;
    @Getter
    @Setter
    private Supplier<? extends List<? extends T>> itemsLoader;
    private final TailingDebouncer valueDebouncer;

    public AzureComboBox() {
        this(true);
    }

    public AzureComboBox(boolean refresh) {
        super();
        this.init();
        this.reloader = new TailingDebouncer(this::doReloadItems, DEBOUNCE_DELAY);
        this.valueDebouncer = new TailingDebouncer(this::fireValueChangedEvent, DEBOUNCE_DELAY);
        if (refresh) {
            this.reloadItems();
        }
    }

    public AzureComboBox(@Nonnull Supplier<? extends List<? extends T>> itemsLoader) {
        this(itemsLoader, true);
    }

    public AzureComboBox(@Nonnull Supplier<? extends List<? extends T>> itemsLoader, boolean refresh) {
        this(refresh);
        this.itemsLoader = itemsLoader;
    }

    protected void init() {
        this.myEditor = new AzureComboBoxEditor();
        this.setEditable(true);
        this.setEditor(this.myEditor);
        this.setLoading(false);
        this.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(@Nonnull final JList<? extends T> l, final T t, final int i, final boolean b,
                                  final boolean b1) {
                setText(getItemText(t));
                setIcon(getItemIcon(t));
            }
        });
        if (isFilterable()) {
            this.addPopupMenuListener(new AzureComboBoxPopupMenuListener());
        }
        this.addItemListener((e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                this.refreshValue();
            }
            this.valueDebouncer.debounce();
        });
        this.trackValidation();
    }

    @Nullable
    @Override
    public T getValue() {
        return ((T) this.getSelectedItem());
    }

    @Nullable
    public Object getRawValue() {
        return this.value;
    }

    @Override
    public void setValue(final T val) {
        this.setValue(val, null);
    }

    public void setValue(final T val, final Boolean fixed) {
        UIUtil.invokeLaterIfNeeded(() -> {
            Optional.ofNullable(fixed).ifPresent(f -> {
                this.setEnabled(!f);
                this.setEditable(!f);
            });
            this.valueNotSet = false;
            this.value = val;
            if (Objects.nonNull(this.value)) {
                final LRUStack<T> history = (LRUStack<T>) CacheManager.getUsageHistory(value.getClass());
                history.push((T) value);
            }
            this.refreshValue();
        });
    }

    public void setValue(final ItemReference<T> val) {
        this.setValue(val, null);
    }

    public void setValue(final ItemReference<T> val, final Boolean fixed) {
        UIUtil.invokeLaterIfNeeded(() -> {
            Optional.ofNullable(fixed).ifPresent(f -> {
                this.setEnabled(!f);
                this.setEditable(!f);
            });
            this.valueNotSet = false;
            this.value = val;
            this.refreshValue();
        });
    }

    protected void refreshValue() {
        if (this.valueNotSet) {
            if (this.getItemCount() > 0 && this.getSelectedIndex() != 0) {
                super.setSelectedItem(this.getDefaultValue());
            }
        } else {
            final Object selected = this.getSelectedItem();
            if (Objects.equals(selected, this.value) || (this.value instanceof ItemReference && ((ItemReference<?>) this.value).is(selected))) {
                return;
            }
            final List<T> items = this.getItems();
            if (this.value instanceof AzureComboBox.ItemReference) {
                items.stream().filter(i -> ((ItemReference<?>) this.value).is(i)).findFirst().ifPresent(this::setValue);
            } else if (items.contains(this.value)) {
                super.setSelectedItem(items.get(items.indexOf(this.value))); // set the equivalent item in the list as selected.
            } else {
                super.setSelectedItem(null);
            }
            this.valueDebouncer.debounce();
        }
    }

    @Override
    public T getDefaultValue() {
        final List<T> items = this.getItems();
        final T value = doGetDefaultValue();
        final int index = items.indexOf(value);
        if (Objects.nonNull(value) && index > -1) {
            return items.get(index);
        } else {
            return this.getModel().getElementAt(0);
        }
    }

    @Nullable
    protected T doGetDefaultValue() {
        final List<T> items = this.getItems();
        //noinspection unchecked
        final LRUStack<T> history = (LRUStack<T>) CacheManager.getUsageHistory(items.get(0).getClass());
        return history.peek();
    }

    @Override
    public void setSelectedItem(final Object value) {
        this.setValue((T) value);
    }

    protected void refreshItems() {
        this.reloadItems();
    }

    public void reloadItems() {
        this.setLoading(true);
        this.reloader.debounce();
    }

    @AzureOperation(name = "common.load_combobox_items.type", params = {"this.getLabel()"}, type = AzureOperation.Type.SERVICE)
    private void doReloadItems() {
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            this.setLoading(true);
            this.setItems(this.loadItemsInner());
            this.setLoading(false);
        });
    }

    public List<T> getItems() {
        final List<T> result = new ArrayList<>();
        for (int i = 0; i < this.getItemCount(); i++) {
            result.add(this.getItemAt(i));
        }
        return result;
    }

    protected synchronized void setItems(final List<? extends T> items) {
        SwingUtilities.invokeLater(() -> {
            final DefaultComboBoxModel<T> model = (DefaultComboBoxModel<T>) this.getModel();
            model.removeAllElements();
            model.addAll(ObjectUtils.firstNonNull(items, Collections.emptyList()));
            this.refreshValue();
        });
    }

    public void clear() {
        this.value = null;
        this.valueNotSet = true;
        this.removeAllItems();
        this.refreshValue();
    }

    protected void setLoading(final boolean loading) {
        this.loading = loading;
        SwingUtilities.invokeLater(() -> {
            this.myEditor.rerender();
            this.repaint();
        });
    }

    @Override
    public void setEnabled(boolean b) {
        this.enabled = b;
        super.setEnabled(b);
        this.setLoading(this.loading);
    }

    @Override
    public boolean isEnabled() {
        return !this.loading && (this.enabled || super.isEnabled());
    }

    protected String getItemText(Object item) {
        if (item == null) {
            return StringUtils.EMPTY;
        }
        return item.toString();
    }

    @Nullable
    protected Icon getItemIcon(Object item) {
        return null;
    }

    @Nonnull
    protected List<Extension> getExtensions() {
        final ArrayList<Extension> list = new ArrayList<>();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.CTRL_DOWN_MASK);
        final String tooltip = String.format("Refresh (%s)", KeymapUtil.getKeystrokeText(keyStroke));
        final Extension refreshEx = Extension.create(AllIcons.Actions.Refresh, tooltip, this::refreshItems);
        this.registerShortcut(keyStroke, refreshEx);
        list.add(refreshEx);
        return list;
    }

    protected void registerShortcut(@Nonnull final KeyStroke keyStroke, @Nonnull final Extension ex) {
        // Add shot cut for extension, refers https://github.com/JetBrains/intellij-community/blob/idea/212.4746.92/platform/platform-api/
        // src/com/intellij/ui/components/fields/ExtendableTextField.java#L117
        final Runnable action = () -> {
            this.hidePopup();
            ex.getActionOnClick().run();
        };
        new DumbAwareAction() {
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                action.run();
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(keyStroke), this);

    }

    protected final List<? extends T> loadItemsInner() {
        try {
            if (Objects.nonNull(this.itemsLoader)) {
                return this.itemsLoader.get();
            } else {
                return this.loadItems();
            }
        } catch (final Exception e) {
            final Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (!(rootCause instanceof InterruptedIOException) && !(rootCause instanceof InterruptedException)) {
                return Collections.emptyList();
            }
            this.handleLoadingError(e);
            return Collections.emptyList();
        }
    }

    @Nonnull
    protected List<? extends T> loadItems() throws Exception {
        return Collections.emptyList();
    }

    protected void handleLoadingError(Throwable e) {
        final Throwable rootCause = ExceptionUtils.getRootCause(e);
        if (rootCause instanceof InterruptedIOException || rootCause instanceof InterruptedException) {
            // Swallow interrupted exception caused by unsubscribe
            return;
        }
        AzureMessager.getMessager().error(e);
    }

    protected boolean isFilterable() {
        return true;
    }

    class AzureComboBoxEditor extends BasicComboBoxEditor {

        private Object item;

        @Override
        public void setItem(Object item) {
            this.item = item;
            if (!AzureComboBox.this.isPopupVisible()) {
                this.editor.setText(getItemText(item));
            }
            if (item == null && AzureComboBox.this.loading) {
                this.editor.setText("Refreshing...");
            }
        }

        @Override
        public Object getItem() {
            return this.item;
        }

        @Override
        protected JTextField createEditorComponent() {
            final ExtendableTextField textField = new ExtendableTextField();
            final List<Extension> extensions = ObjectUtils.firstNonNull(this.getExtensions(), Collections.emptyList());
            extensions.stream().filter(Objects::nonNull).forEach(textField::addExtension);
            textField.setBorder(null);
            textField.setEditable(false);
            return textField;
        }

        @Nullable
        protected List<Extension> getExtensions() {
            if (!AzureComboBox.this.enabled) {
                return Collections.emptyList();
            }
            if (AzureComboBox.this.loading) {
                return List.of(Extension.create(new AnimatedIcon.Default(), "Loading...", null));
            }
            return AzureComboBox.this.getExtensions().stream()
                .map(e -> Extension.create(e.getIcon(false), e.getIcon(true), e.getTooltip(), () -> {
                    AzureComboBox.this.hidePopup(); // hide popup before action.
                    e.getActionOnClick().run();
                })).collect(Collectors.toList());
        }

        public void rerender() {
            ((ExtendableTextField) this.getEditorComponent()).setExtensions(this.getExtensions());
        }
    }

    class AzureComboBoxPopupMenuListener extends PopupMenuListenerAdapter {
        List<T> itemList;
        ComboFilterListener comboFilterListener;

        @Override
        public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
            getEditorComponent().setEditable(true);
            getEditorComponent().setText(StringUtils.EMPTY);
            itemList = AzureComboBox.this.getItems();
            // todo: support customized combo box filter
            comboFilterListener = new ComboFilterListener(itemList,
                (item, input) -> StringUtils.containsIgnoreCase(getItemText(item), input));
            getEditorComponent().getDocument().addDocumentListener(comboFilterListener);
        }

        @Override
        public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
            getEditorComponent().setEditable(false);
            if (comboFilterListener != null) {
                getEditorComponent().getDocument().removeDocumentListener(comboFilterListener);
            }
            final Object selectedItem = AzureComboBox.this.getSelectedItem();
            if (!CollectionUtils.isEqualCollection(itemList, getItems())) {
                AzureComboBox.this.setItems(itemList);
            }
            if (!Objects.equals(selectedItem, AzureComboBox.this.getValue())) {
                AzureComboBox.this.setSelectedItem(selectedItem);
            }
            getEditorComponent().setText(getItemText(selectedItem));
        }

        private JTextField getEditorComponent() {
            return (JTextField) myEditor.getEditorComponent();
        }
    }

    class ComboFilterListener extends DocumentAdapter {

        private final List<? extends T> list;
        private final BiPredicate<? super T, ? super String> filter;

        public ComboFilterListener(List<? extends T> list, BiPredicate<? super T, ? super String> filter) {
            super();
            this.list = list;
            this.filter = filter;
        }

        @Override
        protected void textChanged(@Nonnull final DocumentEvent documentEvent) {
            SwingUtilities.invokeLater(() -> {
                try {
                    final String text = documentEvent.getDocument().getText(0, documentEvent.getDocument().getLength());
                    list.stream().filter(item -> filter.test(item, text) && !getItems().contains(item)).forEach(AzureComboBox.this::addItem);
                    getItems().stream().filter(item -> !filter.test(item, text)).forEach(AzureComboBox.this::removeItem);
                } catch (BadLocationException e) {
                    // swallow exception and show all items
                }
            });
        }
    }

    public static class ItemReference<T> {
        private final Predicate<? super T> predicate;

        public ItemReference(@Nonnull Predicate<? super T> predicate) {
            this.predicate = predicate;
        }

        public ItemReference(@Nonnull Object val, Function<T, ?> mapper) {
            this.predicate = t -> Objects.equals(val, mapper.apply(t));
        }

        public boolean is(Object obj) {
            if (Objects.isNull(obj)) {
                return false;
            }
            return this.predicate.test((T) obj);
        }
    }
}
