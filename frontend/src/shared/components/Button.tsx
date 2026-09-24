import type { ButtonHTMLAttributes } from 'react'
import { buttonClassName, type ButtonStyleProps } from './buttonStyles'

interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement>, ButtonStyleProps {}

/** Botón de acción real (submit, click con `onClick`) — nunca navega. Para eso, {@link LinkButton}. */
export function Button({ variant, small, className, ...props }: ButtonProps) {
  return (
    <button
      className={buttonClassName({ variant, small }, className)}
      {...props}
    />
  )
}
