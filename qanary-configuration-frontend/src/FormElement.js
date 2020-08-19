import React, {Component} from "react";
import './styling.css';

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
        return (
            <div>
                <label className={'label'}>{this.state.name}:
                    <input value={this.state.value} onChange={this.handleChange} className={'input'}/>
                </label>
            </div>
        )
    }
}