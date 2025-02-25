    /**
     * Simple panel for non nested data like General information, study, etc.
     */
    class FormPanel {

        constructor(title, formData, data, port) {
            _log( 'FormPanel /'+title, 'primary' );
            let O = this;
            O.panel = $( '<div class="panel-body"></div>' ) 
            O.inputs = {};

            O._create(title, formData, data, port);
        }

        /**
         * ```
         * <div class="panel panel-default">
         *   <div class="panel-heading">
         *     <h3 class="panel-title">Some title</h3>
         *   </div>
         *   <div class="panel-body">
         *     <form></form>
         *   </div>
         * </div>
         * ```
         * @param {*} title 
         * @param {*} formData 
         */
        _create(title, formData, data, port) {
            let O = this;
            let form = $( '<form class="form-striped"></form>' )
            formData.forEach(prop => {
                let inputForm = createForm(prop, data ? data[prop.id] : null, port, title ==="Parameter"? true : false);
                if (inputForm) {
                    $(inputForm.group).appendTo( form )
                    O.inputs[prop.id] = inputForm;
                }
            });
            form.appendTo( O.panel)
            // init form items' functions: touchspin, range, select2 ...
			_appUI._initFormItems( form);
        }

        validate() {
            let O = this;
            let isValid = true;
            Object.values(O.inputs).forEach(input => {
                if (!input.validate()) isValid = false;
            });
            return isValid;
        }

        get data() {
            let O = this;
            let data = {};
            Object.entries(O.inputs).forEach(([id, input]) => data[id] = input.value);
            return data;
        }
    }