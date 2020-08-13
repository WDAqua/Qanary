import React, {Component} from "react";

export default class FormElement extends Component{

    state = {
        name: this.props.name,
        value: this.props.value
    };

    handleChange = (event) => {
        this.setState({value: event.target.value}, () => {
            this.props.onChange(this.state)
        })
    };



    render() {
        const style = {
            'margin-left': '1rem',
            'margin-bottom': '2px',
            'margin-top': '3px',
            'width': '300px'
        }

        return (
            <div>
                <label style={style}>{this.state.name}:
                    <input value={this.state.value} onChange={this.handleChange} style={style}/>
                </label>
            </div>
        )
    }
}