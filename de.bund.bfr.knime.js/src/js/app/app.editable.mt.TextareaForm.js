    /**
     * Create a Bootstrap 3 form-group for a textarea. 
     */
    class TextareaForm {

        /**
         * Create a Bootstrap 3 form-group.
         * 
         * ```
         * <div class="form-group row">
         *   <label>name</label>
         *   <textarea class="form-control" rows="3"></textarea>
         * </div>
         * ```
         */
        
        constructor(name, mandatory, helperText, value) {
            let O = this;
            O.name = name;
            O.mandatory = mandatory;
            O.helperText = helperText;
            
            O.textarea = $( '<textarea row="6" class="form-control form-control-sm" />' )
                            .attr( 'id', 'area_'+ name.replace(/[\W_]+/g,"_") );
            O._create(name, mandatory, helperText, value);
        }

        /**
         * @param {string} name Property name
         * @param {boolean} mandatory `true` if mandatory, `false` if optional.
         * @param {string} helperText Tooltip
         * @param {string} value Initial value of the property.
         */
        _create(name, mandatory, helperText, value) {
            let O = this;
            // formgroup
            let $formGroup = $( '<div class="form-group row"></div>' );

            // label
            let $label = $( '<label class="col-form-label col-form-label-sm col-9 col-xs-3 order-1 sim-param-label"></label>' )
                .attr( 'for', 'areaInput_'+ name.replace(/[\W_]+/g,"_") )
                .appendTo( $formGroup );
            $label.text(name+(mandatory?"*":""));
            
            // field
            let $field = $( '<div class="col-12 col-xs-7 col-md-6 order-3 order-xs-2 "></div>' )
                .appendTo( $formGroup );

            // actions
            let $actions = $( '<div class="col-3 col-xs-auto order-2 order-xs-3 sim-param-actions"></div>' )
                .appendTo( $formGroup );

            // input item
            O.input = null;

            // create param metadata action
            if (helperText) {
                // action metadata list
                let $actionMetadata = $( '<button class="action action-pure float-right" type="button"><i class="feather icon-info"></i></button>' )
                    .attr( 'data-toggle', 'collapse' )
                    .attr( 'data-target', '#paramMetadata_'+ name.replace(/[\W_]+/g,"_") )
                    .attr( 'aria-expanded', false )
                    .attr( 'aria-controls', 'paramMetadata_'+ name.replace(/[\W_]+/g,"_") )
                    .attr( 'title', 'Show Metadata' )
                    .appendTo( $actions );
            }

            O.input  = $( '<textarea type="text" row="6" class="form-control" />' )
                      .attr( 'id', 'areaInput_'+ name.replace(/[\W_]+/g,"_") )
                            .appendTo( $field );
            O.input.val(value);

            // create validation container
            O.input.$validationContainer = $( '<div class="validation-message mt-1"></div>' )
            .appendTo( $field );

            // create param metadata list
            if (helperText) {
                // metadata table
                let $metadataContainer = $( '<div class="collapse param-metadata"></div>' )
                    .attr( 'id', 'paramMetadata_'+ name.replace(/[\W_]+/g,"_") )
                    .attr( 'aria-expanded', false )
                    .appendTo( $field );

                $metadataContainer.append( _createHelperMetadataText( helperText ) );
            }
            O.group =  $formGroup;
        }
       
        get value() {
            let O = this;
            return O.input.val();
        }

        set value(newValue) {
            let O = this;
            O.input.val(newValue);
        }
        onblurHandler(){
            let O = this;
            let closestForm = O.input.closest( "form" );
            let attr = closestForm.attr('no-immidiate-submit' );
            let can_emit_Event = typeof attr === typeof undefined || attr === false;
            _log( ' onblurHandler' + can_emit_Event );
            if ( can_emit_Event ) { 
                window.editEventBus.broadcast('MetadataChanged');
            }
        }
        clear() {
            let O = this;
            O.input.val( "" );
        }

        /**
         * @return {boolean} If the textarea is valid.
         */
        validate() {
            let O = this;
            let isValid;
            O.input.find( '.has-error' ).removeClass( 'has-error' );
            O.input.find( '.is-invalid' ).removeClass( 'is-invalid' );
            O.input.find( '.validation-message' ).empty();
            if (!O.mandatory) {
                isValid = true;
            } else {
                isValid = O.input.val() ? true : false;
            }
            if (!isValid) {
                O.input.$validationContainer.text(`required`);
                O.input.parents( '.form-group' ).addClass( 'has-error' );
			    O.input.addClass( 'is-invalid' );
                O.input.$validationContainer.css("display", "block") ;
            }else{
                O.onblurHandler();
            }
            return isValid;
        }
    }