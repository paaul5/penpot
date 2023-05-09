;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns app.main.ui.workspace.sidebar.component.sidebar
  (:require-macros [app.main.style :refer [css]])
  (:require
   [app.main.data.workspace :as dw]
   [app.main.refs :as refs]
   [app.main.store :as st]
   [app.main.ui.components.tab-container.tab-container :refer [tab-container tab-element]] 
   [app.main.ui.context :as ctx]
   [app.main.ui.hooks.resize :refer [use-resize-hook]]
   [app.main.ui.icons :as i]
   [app.main.ui.workspace.comments :refer [comments-sidebar]]
   [app.main.ui.workspace.sidebar.assets :refer [assets-toolbox]]
   [app.main.ui.workspace.sidebar.debug :refer [debug-panel]]
   [app.main.ui.workspace.sidebar.history :refer [history-toolbox]]
   [app.main.ui.workspace.sidebar.layers.layers :refer [layers-toolbox]]
   [app.main.ui.workspace.sidebar.options :refer [options-toolbox]]
   [app.main.ui.workspace.sidebar.shortcuts :refer [shortcuts-container]]
   [app.main.ui.workspace.sidebar.sitemap.sitemap :refer [sitemap]]
   [app.util.dom :as dom]
   [app.util.i18n :refer [tr]]
   [app.util.object :as obj]
   [rumext.v2 :as mf]))

;; --- Left Sidebar (Component)

(mf/defc left-sidebar
  {:wrap [mf/memo]}
  [{:keys [layout] :as props}]
  (let [options-mode        (mf/deref refs/options-mode-global)
        mode-inspect?       (= options-mode :inspect)
        section             (cond (or mode-inspect? (contains? layout :layers)) :layers
                                  (contains? layout :assets) :assets)
        shortcuts? (contains? layout :shortcuts)
        show-debug? (contains? layout :debug-panel)
        new-css-system (mf/use-ctx ctx/new-css-system)
        {:keys [on-pointer-down on-lost-pointer-capture on-pointer-move parent-ref size]}
        (use-resize-hook :left-sidebar 255 255 500 :x false :left)

        handle-collapse
        (fn []
          (st/emit! (dw/toggle-layout-flag :collapse-left-sidebar)))]

    [:aside {:ref parent-ref
             :class (if new-css-system
                      (dom/classnames (css :left-settings-bar) true)
                      (dom/classnames :settings-bar true
                                      :settings-bar-left true
                                      :two-row   (<= size 300)
                                      :three-row (and (> size 300) (<= size 400))
                                      :four-row  (> size 400)))
             :style #js {"--width" (str size "px")}}

     [:div {:on-pointer-down on-pointer-down
            :on-lost-pointer-capture on-lost-pointer-capture
            :on-pointer-move on-pointer-move
            :class (if new-css-system
                     (dom/classnames (css :resize-area) true)
                     (dom/classnames :resize-area true))}]
     [:div {:class (if new-css-system
                     (dom/classnames (css :settings-bar-inside) true)
                     (dom/classnames :settings-bar-inside true))}
      (cond
        shortcuts?
        [:& shortcuts-container]

        show-debug?
        [:& debug-panel]

        :else
        [:*

         (if new-css-system
           [:& tab-container {:on-change-tab #(st/emit! (dw/go-to-layout %))
                              :selected section
                              :shortcuts? shortcuts?
                              :collapsable? true
                              :handle-collapse handle-collapse}
            [:& tab-element {:id :layers
                             :title (tr "workspace.sidebar.layers")}
             [:div {:class (dom/classnames (css :layers-tab) true)}
              [:& sitemap {:layout layout}]
              [:& layers-toolbox {:size-parent size}]]]

            (when-not mode-inspect?
              [:& tab-element {:id :assets :title (tr "workspace.toolbar.assets")}
               [:& assets-toolbox]])]

           [:*
            [:button.collapse-sidebar
             {:on-click handle-collapse
              :aria-label (tr "workspace.sidebar.collapse")}
             i/arrow-slide]
            [:& tab-container {:on-change-tab #(st/emit! (dw/go-to-layout %))
                                :selected section
                                :shortcuts? shortcuts?
                                :collapsable? true
                                :handle-collapse handle-collapse}
             [:& tab-element {:id :layers
                               :title (tr "workspace.sidebar.layers")}
              [:div {:class (dom/classnames :layers-tab true)}
               [:& sitemap {:layout layout}]
               [:& layers-toolbox {:size-parent size}]]]

             (when-not mode-inspect?
               [:& tab-element {:id :assets :title (tr "workspace.toolbar.assets")}
                [:& assets-toolbox]])]])])]]))

;; --- Right Sidebar (Component)

(mf/defc right-sidebar
  {::mf/wrap-props false
   ::mf/wrap [mf/memo]}
  [props]
  (let [layout          (obj/get props "layout")
        section         (obj/get props "section")
        drawing-tool    (:tool (mf/deref refs/workspace-drawing))

        is-comments?    (= drawing-tool :comments)
        is-history?     (contains? layout :document-history)
        is-inspect?     (= section :inspect)

        expanded?        (mf/deref refs/inspect-expanded)
        can-be-expanded? (and
                          (not is-comments?)
                          (not is-history?)
                          is-inspect?)]

    (mf/use-effect
     (mf/deps can-be-expanded?)
     (fn []
       (when (not can-be-expanded?)
         (st/emit! (dw/set-inspect-expanded false)))))

    [:aside.settings-bar.settings-bar-right {:class (when (and can-be-expanded? expanded?) "expanded")}
     [:div.settings-bar-inside
      (cond
        is-comments?
        [:& comments-sidebar]

        is-history?
        [:& history-toolbox]

        :else
        [:> options-toolbox props])]]))
